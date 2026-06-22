package com.opiagile.supportai.lead;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RuleBasedIntentClassifierTest {

    private final RuleBasedIntentClassifier classifier = new RuleBasedIntentClassifier();

    @Test
    void deveClassificarPedidoDeAgendamento() {
        assertThat(classifier.classify("Quero agendar uma consulta para sexta"))
                .isEqualTo(Intent.AGENDAR);
    }

    @Test
    void devePriorizarRemarcacaoAntesDeAgendamento() {
        assertThat(classifier.classify("Preciso remarcar minha consulta"))
                .isEqualTo(Intent.REMARCAR);
    }

    @Test
    void deveClassificarEmergenciaComoForaDoEscopo() {
        assertThat(classifier.classify("Vocês fazem cirurgia de emergência?"))
                .isEqualTo(Intent.FORA_DO_ESCOPO);
    }

    @Test
    void deveClassificarPedidoDeHumano() {
        assertThat(classifier.classify("Quero falar com um atendente humano"))
                .isEqualTo(Intent.FALAR_COM_HUMANO);
    }

    @Test
    void deveClassificarReclamacao() {
        assertThat(classifier.classify("Estou insatisfeito e quero fazer uma reclamação"))
                .isEqualTo(Intent.RECLAMACAO);
    }
}
