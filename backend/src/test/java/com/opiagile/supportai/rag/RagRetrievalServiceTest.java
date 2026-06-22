package com.opiagile.supportai.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.opiagile.supportai.tenant.TenantContext;

@ExtendWith(MockitoExtension.class)
class RagRetrievalServiceTest {

    @Mock
    private RagChunkRepository chunkRepository;

    @Mock
    private EmbeddingProvider embeddingProvider;

    private final TextSimilarityScorer scorer = new TextSimilarityScorer();
    private final TenantContext tenantContext = new TenantContext(
            UUID.randomUUID(),
            "demo",
            UUID.randomUUID(),
            "clinica-demo",
            "Clínica Demo");

    @Test
    void deveUsarRecuperacaoTextualQuandoEmbeddingNaoEstiverDisponivel() {
        when(embeddingProvider.embed("atendimento aos sábados")).thenReturn(Optional.empty());
        when(chunkRepository.findAllIndexedChunks(tenantContext)).thenReturn(List.of(chunk(
                "A clínica atende aos sabados das 8h as 12h com horário agendado.",
                null)));

        RagRetrievalService service = new RagRetrievalService(chunkRepository, scorer, embeddingProvider, 5, 0.15);

        List<RetrievedChunk> chunks = service.retrieve(tenantContext, "atendimento aos sábados");

        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().retrievalProvider()).isEqualTo("local-text");
        verify(chunkRepository, never()).findNearestIndexedChunks(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void devePriorizarPgvectorQuandoEmbeddingDaConsultaExistir() {
        float[] embedding = new float[] { 0.1f, 0.2f };
        when(embeddingProvider.embed("horário sábado")).thenReturn(Optional.of(embedding));
        when(embeddingProvider.providerName()).thenReturn("openai");
        when(chunkRepository.findNearestIndexedChunks(tenantContext, embedding, 5)).thenReturn(List.of(chunk(
                "Atendemos aos sábados das 8h às 12h.",
                0.91)));

        RagRetrievalService service = new RagRetrievalService(chunkRepository, scorer, embeddingProvider, 5, 0.15);

        List<RetrievedChunk> chunks = service.retrieve(tenantContext, "horário sábado");

        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().score()).isEqualTo(0.91);
        assertThat(chunks.getFirst().retrievalProvider()).isEqualTo("pgvector-openai");
        verify(chunkRepository, never()).findAllIndexedChunks(tenantContext);
    }

    @Test
    void deveVoltarParaTextoQuandoPgvectorNaoRetornarResultados() {
        float[] embedding = new float[] { 0.1f, 0.2f };
        when(embeddingProvider.embed("documentos consulta")).thenReturn(Optional.of(embedding));
        when(chunkRepository.findNearestIndexedChunks(tenantContext, embedding, 5)).thenReturn(List.of());
        when(chunkRepository.findAllIndexedChunks(tenantContext)).thenReturn(List.of(chunk(
                "Para consulta, leve documentos pessoais, documento com foto e carteirinha do convênio.",
                null)));

        RagRetrievalService service = new RagRetrievalService(chunkRepository, scorer, embeddingProvider, 5, 0.15);

        List<RetrievedChunk> chunks = service.retrieve(tenantContext, "documentos consulta");

        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().retrievalProvider()).isEqualTo("local-text");
    }

    private StoredChunk chunk(String content, Double score) {
        return new StoredChunk(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "faq.txt",
                content,
                score);
    }
}
