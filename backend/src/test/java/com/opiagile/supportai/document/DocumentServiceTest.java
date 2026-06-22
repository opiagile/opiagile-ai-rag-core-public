package com.opiagile.supportai.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.opiagile.supportai.rag.EmbeddingProvider;

class DocumentServiceTest {

    private final DocumentRepository documentRepository = mock(DocumentRepository.class);
    private final DocumentChunkRepository chunkRepository = mock(DocumentChunkRepository.class);
    private final EmbeddingProvider embeddingProvider = mock(EmbeddingProvider.class);

    @Test
    void deveRejeitarArquivoMaiorQueLimiteConfigurado() {
        DocumentService service = service(1024, 2000, 10);
        MockMultipartFile file = txt("faq.txt", "a".repeat(1025));

        assertThatThrownBy(() -> service.upload(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Arquivo muito grande");

        verify(documentRepository, never()).save(anyString(), anyString(), anyString(), any());
    }

    @Test
    void deveRejeitarTxtComUtf8Invalido() {
        DocumentService service = service(1024, 1000, 10);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "faq.txt",
                "text/plain",
                new byte[] {(byte) 0xC3, (byte) 0x28});

        assertThatThrownBy(() -> service.upload(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UTF-8");
    }

    @Test
    void deveRejeitarTextoAcimaDoLimiteDeCaracteres() {
        DocumentService service = service(1024, 120, 10);
        MockMultipartFile file = txt("faq.txt", "a".repeat(121));

        assertThatThrownBy(() -> service.upload(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("120 caracteres");
    }

    @Test
    void deveRejeitarArquivoQueGeraChunksDemais() {
        DocumentService service = service(4096, 1000, 1);
        MockMultipartFile file = txt("faq.txt", "texto ".repeat(80));

        assertThatThrownBy(() -> service.upload(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chunks demais");
    }

    @Test
    void deveSanitizarNomeDoArquivoAntesDePersistir() {
        UUID documentId = UUID.randomUUID();
        when(documentRepository.save(anyString(), eq("text/plain"), eq("UPLOAD"), eq(DocumentStatus.INDEXED)))
                .thenReturn(new DocumentRecord(
                        documentId,
                        "FAQ ___2026.txt",
                        "text/plain",
                        "UPLOAD",
                        DocumentStatus.INDEXED.name(),
                        OffsetDateTime.now()));
        when(chunkRepository.save(eq(documentId), anyInt(), anyString(), anyString(), eq(Optional.empty())))
                .thenReturn(new DocumentChunkRecord(
                        UUID.randomUUID(),
                        documentId,
                        0,
                        "conteudo",
                        "{}",
                        OffsetDateTime.now()));
        when(embeddingProvider.embed(anyString())).thenReturn(Optional.empty());
        when(embeddingProvider.providerName()).thenReturn("noop");

        DocumentService service = service(1024, 1000, 10);
        DocumentUploadResponse response = service.upload(txt("../FAQ ç<>2026.txt", "Conteudo valido para demo."));

        assertThat(response.documentId()).isEqualTo(documentId);
        assertThat(response.filename()).isEqualTo("FAQ ___2026.txt");
        assertThat(response.chunkCount()).isEqualTo(1);
        verify(documentRepository).save("FAQ ___2026.txt", "text/plain", "UPLOAD", DocumentStatus.INDEXED);
    }

    private DocumentService service(long maxUploadBytes, int maxUploadChars, int maxUploadChunks) {
        return new DocumentService(
                documentRepository,
                chunkRepository,
                new DocumentChunker(100, 0),
                embeddingProvider,
                maxUploadBytes,
                maxUploadChars,
                maxUploadChunks);
    }

    private MockMultipartFile txt(String filename, String content) {
        return new MockMultipartFile(
                "file",
                filename,
                "text/plain",
                content.getBytes(StandardCharsets.UTF_8));
    }
}
