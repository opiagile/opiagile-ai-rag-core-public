package com.opiagile.supportai.document;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.opiagile.supportai.rag.EmbeddingProvider;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentChunker chunker;
    private final EmbeddingProvider embeddingProvider;
    private final long maxUploadBytes;
    private final int maxUploadChars;
    private final int maxUploadChunks;
    private final int maxDocuments;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentChunkRepository chunkRepository,
            DocumentChunker chunker,
            EmbeddingProvider embeddingProvider,
            @Value("${documents.upload.max-bytes:262144}") long maxUploadBytes,
            @Value("${documents.upload.max-chars:200000}") int maxUploadChars,
            @Value("${documents.upload.max-chunks:300}") int maxUploadChunks,
            @Value("${documents.upload.max-documents:50}") int maxDocuments) {
        if (maxUploadBytes < 1024) {
            throw new IllegalArgumentException("documents.upload.max-bytes deve ser maior ou igual a 1024");
        }
        if (maxUploadChars < 100) {
            throw new IllegalArgumentException("documents.upload.max-chars deve ser maior ou igual a 100");
        }
        if (maxUploadChunks < 1) {
            throw new IllegalArgumentException("documents.upload.max-chunks deve ser maior ou igual a 1");
        }
        if (maxDocuments < 1) {
            throw new IllegalArgumentException("documents.upload.max-documents deve ser maior ou igual a 1");
        }
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.chunker = chunker;
        this.embeddingProvider = embeddingProvider;
        this.maxUploadBytes = maxUploadBytes;
        this.maxUploadChars = maxUploadChars;
        this.maxUploadChunks = maxUploadChunks;
        this.maxDocuments = maxDocuments;
    }

    @Transactional
    public DocumentUploadResponse upload(MultipartFile file) {
        validateTxt(file);
        if (documentRepository.countDocuments() >= maxDocuments) {
            throw new IllegalArgumentException("Limite de documentos da demo atingido. Remova dados de demonstração antes de enviar novo arquivo.");
        }
        String content = readContent(file);
        List<String> chunks = chunker.chunk(content);
        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("O arquivo TXT não possui conteúdo indexável.");
        }
        if (chunks.size() > maxUploadChunks) {
            throw new IllegalArgumentException("O arquivo gerou chunks demais para a demo. Reduza o conteúdo e tente novamente.");
        }

        DocumentRecord document = documentRepository.save(
                safeFilename(file),
                contentType(file),
                "UPLOAD",
                DocumentStatus.INDEXED);

        for (int i = 0; i < chunks.size(); i++) {
            Optional<float[]> embedding = embeddingProvider.embed(chunks.get(i));
            chunkRepository.save(document.id(), i, chunks.get(i), metadataJson(document.filename(), i), embedding);
        }

        return new DocumentUploadResponse(
                document.id(),
                document.filename(),
                document.status(),
                chunks.size(),
                embeddingProvider.providerName());
    }

    public List<DocumentSummaryResponse> findAll() {
        return documentRepository.findAll().stream()
                .map(document -> new DocumentSummaryResponse(
                        document.id(),
                        document.filename(),
                        document.contentType(),
                        document.sourceType(),
                        document.status(),
                        documentRepository.countChunks(document.id()),
                        document.createdAt()))
                .toList();
    }

    public DocumentDetailResponse findById(UUID id) {
        DocumentRecord document = documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Documento não encontrado: " + id));
        return new DocumentDetailResponse(
                document.id(),
                document.filename(),
                document.contentType(),
                document.sourceType(),
                document.status(),
                documentRepository.countChunks(document.id()),
                document.createdAt());
    }

    public List<DocumentChunkResponse> findChunks(UUID documentId) {
        findById(documentId);
        return chunkRepository.findByDocumentId(documentId).stream()
                .map(chunk -> new DocumentChunkResponse(
                        chunk.id(),
                        chunk.documentId(),
                        chunk.chunkIndex(),
                        chunk.content(),
                        chunk.metadata(),
                        chunk.createdAt()))
                .toList();
    }

    private void validateTxt(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Envie um arquivo TXT com conteúdo.");
        }
        String filename = safeFilename(file).toLowerCase();
        String contentType = contentType(file).toLowerCase();
        boolean txtFilename = filename.endsWith(".txt");
        boolean textContentType = contentType.startsWith("text/") || contentType.equals("application/octet-stream");
        if (!txtFilename || !textContentType) {
            throw new IllegalArgumentException("Nesta fase, apenas arquivos .txt são suportados.");
        }
        if (file.getSize() > maxUploadBytes) {
            throw new IllegalArgumentException("Arquivo muito grande. O limite atual para a demo é " + humanBytes(maxUploadBytes) + ".");
        }
    }

    private String readContent(MultipartFile file) {
        try {
            String content = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(file.getBytes()))
                    .toString();
            if (content.length() > maxUploadChars) {
                throw new IllegalArgumentException("Arquivo com texto demais para a demo. O limite atual é " + maxUploadChars + " caracteres.");
            }
            return content;
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("O arquivo TXT deve estar codificado em UTF-8 válido.");
        } catch (IOException exception) {
            throw new IllegalArgumentException("Não foi possível ler o arquivo enviado.");
        }
    }

    private String safeFilename(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            return "documento.txt";
        }
        String normalizedFilename = filename.replace("\\", "/");
        String safeName = normalizedFilename.substring(normalizedFilename.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "")
                .replaceAll("[^A-Za-z0-9._ -]", "_")
                .trim();
        if (safeName.isBlank()) {
            return "documento.txt";
        }
        return safeName.length() > 120 ? safeName.substring(0, 120) : safeName;
    }

    private String contentType(MultipartFile file) {
        return file.getContentType() == null ? "text/plain" : file.getContentType();
    }

    private String metadataJson(String filename, int chunkIndex) {
        return "{\"filename\":\"" + escapeJson(filename) + "\",\"chunkIndex\":" + chunkIndex + "}";
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String humanBytes(long bytes) {
        if (bytes >= 1024 * 1024) {
            return (bytes / (1024 * 1024)) + " MB";
        }
        if (bytes >= 1024) {
            return (bytes / 1024) + " KB";
        }
        return bytes + " bytes";
    }
}
