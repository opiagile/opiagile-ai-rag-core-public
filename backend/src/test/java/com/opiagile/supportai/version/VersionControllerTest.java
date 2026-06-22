package com.opiagile.supportai.version;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class VersionControllerTest {

    @Test
    void deveRetornarMetadadosDaVersao() {
        Clock clock = Clock.fixed(Instant.parse("2026-05-28T00:00:00Z"), ZoneOffset.UTC);
        VersionController controller = new VersionController(
                "opiagile-ai-rag-core",
                "0.1.0-SNAPSHOT",
                "test",
                clock);

        VersionResponse response = controller.version();

        assertThat(response.appName()).isEqualTo("opiagile-ai-rag-core");
        assertThat(response.version()).isEqualTo("0.1.0-SNAPSHOT");
        assertThat(response.environment()).isEqualTo("test");
        assertThat(response.javaVersion()).isNotBlank();
        assertThat(response.timestamp().toInstant()).isEqualTo(Instant.parse("2026-05-28T00:00:00Z"));
    }
}
