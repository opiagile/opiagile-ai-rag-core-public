package com.opiagile.supportai.document;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DocumentChunkerTest {

    @Test
    void deveRetornarChunkUnicoQuandoConteudoForPequeno() {
        DocumentChunker chunker = new DocumentChunker(200, 20);

        assertThat(chunker.chunk("Pergunta frequente sobre atendimento."))
                .containsExactly("Pergunta frequente sobre atendimento.");
    }

    @Test
    void deveDividirConteudoGrandeComSobreposicao() {
        DocumentChunker chunker = new DocumentChunker(120, 20);
        String content = "A".repeat(80) + " " + "B".repeat(80) + " " + "C".repeat(80);

        assertThat(chunker.chunk(content))
                .hasSizeGreaterThan(1)
                .allSatisfy(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(120));
    }

    @Test
    void deveIgnorarConteudoEmBranco() {
        DocumentChunker chunker = new DocumentChunker(200, 20);

        assertThat(chunker.chunk("  \n\n  ")).isEmpty();
    }
}
