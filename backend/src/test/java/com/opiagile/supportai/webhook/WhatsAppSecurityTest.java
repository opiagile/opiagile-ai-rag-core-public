package com.opiagile.supportai.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

class WhatsAppSecurityTest {

    @Test
    void deveNormalizarEMascararTelefone() {
        assertThat(PhoneNumberMasker.normalize("+55 (11) 99999-8888")).isEqualTo("5511999998888");
        assertThat(PhoneNumberMasker.mask("+55 (11) 99999-8888")).isEqualTo("5511****8888");
    }

    @Test
    void allowlistDeveCompararNumerosNormalizados() {
        WhatsAppProperties properties = new WhatsAppProperties();
        properties.setAllowedTestNumbers(List.of("+55 (11) 99999-8888"));
        WhatsAppTesterAllowlistService service = new WhatsAppTesterAllowlistService(properties);

        assertThat(service.isAllowed("5511999998888")).isTrue();
        assertThat(service.isAllowed("5511888887777")).isFalse();
        assertThat(service.allowedCount()).isEqualTo(1);
    }

    @Test
    void assinaturaValidaDevePassar() {
        WhatsAppProperties properties = new WhatsAppProperties();
        properties.setAppSecret("segredo-local");
        properties.setSignatureRequired(true);
        WhatsAppSignatureVerifier verifier = new WhatsAppSignatureVerifier(properties);
        byte[] body = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
        String signature = "sha256=" + verifier.hmacSha256(body, "segredo-local");

        assertThat(verifier.verify(body, signature)).isTrue();
        assertThat(verifier.verify(body, "sha256=errada")).isFalse();
        assertThat(verifier.verify(body, null)).isFalse();
    }

    @Test
    void rateLimitDeveBloquearExcessoPorTelefone() {
        WhatsAppProperties properties = new WhatsAppProperties();
        properties.setRateLimitPerMinute(2);
        WhatsAppRateLimiter limiter = new WhatsAppRateLimiter(properties, Clock.fixed(Instant.parse("2026-05-29T10:00:00Z"), ZoneOffset.UTC));

        assertThat(limiter.allow("5511999998888")).isTrue();
        assertThat(limiter.allow("5511999998888")).isTrue();
        assertThat(limiter.allow("5511999998888")).isFalse();
        assertThat(limiter.allow("5511888887777")).isTrue();
    }
}
