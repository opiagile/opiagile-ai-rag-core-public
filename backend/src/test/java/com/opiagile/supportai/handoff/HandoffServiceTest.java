package com.opiagile.supportai.handoff;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opiagile.supportai.lead.Intent;

class HandoffServiceTest {

    private final HandoffService handoffService = new HandoffService(null, null, null);

    @Test
    void deveGerarMotivoParaPedidoDeHumano() {
        assertThat(handoffService.reason(Intent.FALAR_COM_HUMANO, false))
                .contains("humano");
    }

    @Test
    void deveGerarMotivoParaAusenciaDeFontes() {
        assertThat(handoffService.reason(Intent.DUVIDA_FAQ, true))
                .contains("fontes suficientes");
    }
}
