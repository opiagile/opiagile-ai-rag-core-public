package com.opiagile.supportai.rag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MultilingualQueryExpanderTest {

    private final MultilingualQueryExpander expander = new MultilingualQueryExpander();

    @Test
    void deveExpandirConsultaEmInglesComTermosDeNegocioEmPortugues() {
        String expanded = expander.expand("What services do you offer for WhatsApp integration?", "ENGLISH");

        assertThat(expanded).contains("servicos");
        assertThat(expanded).contains("integracao");
        assertThat(expanded).contains("whatsapp");
    }

    @Test
    void deveExpandirConsultaEmEspanholComTermosDeContatoEmPortugues() {
        String expanded = expander.expand("Quiero hablar con una persona sobre una propuesta", "SPANISH");

        assertThat(expanded).contains("pessoa");
        assertThat(expanded).contains("proposta");
    }

    @Test
    void devePreservarConsultaEmPortuguesSemExpansao() {
        String query = "Como a Opiagile ajuda no atendimento?";

        assertThat(expander.expand(query, "PORTUGUESE")).isEqualTo(query);
    }

    @Test
    void deveExpandirConsultaEmInglesComTermosDeLocacao() {
        String expanded = expander.expand("What happens if I want to cancel a rental?", "ENGLISH");

        assertThat(expanded).contains("cancelamento");
        assertThat(expanded).contains("rescisao");
        assertThat(expanded).contains("locacao");
    }
}
