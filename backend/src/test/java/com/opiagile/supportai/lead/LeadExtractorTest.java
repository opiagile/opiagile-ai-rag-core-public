package com.opiagile.supportai.lead;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LeadExtractorTest {

    private final LeadExtractor extractor = new LeadExtractor();

    @Test
    void deveExtrairNomeTelefoneEmailEInteresse() {
        LeadExtraction extraction = extractor.extract(
                "Meu nome é João Silva, telefone (11) 99999-8888, email joao@email.com. Quero agendar consulta.",
                Intent.AGENDAR);

        assertThat(extraction.name()).isEqualTo("João Silva");
        assertThat(extraction.phone()).contains("99999");
        assertThat(extraction.email()).isEqualTo("joao@email.com");
        assertThat(extraction.interest()).contains("agendar consulta");
    }

    @Test
    void deveIgnorarInteresseParaDuvidaFaq() {
        LeadExtraction extraction = extractor.extract("Quais documentos preciso levar?", Intent.DUVIDA_FAQ);

        assertThat(extraction.interest()).isNull();
    }
}
