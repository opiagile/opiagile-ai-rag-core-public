package com.opiagile.supportai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SupportAiApplicationTests {

    @Test
    void deveExporClassePrincipalDaAplicacao() {
        assertThat(SupportAiApplication.class).isNotNull();
    }
}
