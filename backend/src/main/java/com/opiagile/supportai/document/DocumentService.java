package com.opiagile.supportai.document;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentChunkRepository chunkRepository,
            DocumentChunker chunker,
            EmbeddingProvider embeddingProvider) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.chunker = chunker;
        this.embeddingProvider = embeddingProvider;
    }

    @Transactional
    public DocumentUploadResponse upload(MultipartFile file) {
        validateTxt(file);
        String content = readContent(file);
        List<String> chunks = chunker.chunk(content);
        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("O arquivo TXT não possui conteúdo indexável.");
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
    }

    private String readContent(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
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
        return normalizedFilename.substring(normalizedFilename.lastIndexOf('/') + 1);
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
}
