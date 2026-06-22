package com.opiagile.supportai.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class OfficialCloudApiWhatsAppProviderTest {

    @Test
    void dryRunNaoEnviaMensagemReal() {
        OfficialCloudApiWhatsAppProvider provider = provider(properties(true, true, "token", "12345", List.of("5511999998888")));

        WhatsAppSendResult result = provider.sendText("5511999998888", "Olá");

        assertThat(result.status()).isEqualTo("DRY_RUN");
        assertThat(result.sent()).isFalse();
        assertThat(result.dryRun()).isTrue();
    }

    @Test
    void envioDesabilitadoBloqueiaChamadaReal() {
        WhatsAppProperties properties = properties(false, false, "token", "12345", List.of("5511999998888"));
        OfficialCloudApiWhatsAppProvider provider = provider(properties);

        WhatsAppSendResult result = provider.sendText("5511999998888", "Olá");

        assertThat(result.status()).isEqualTo("SKIPPED");
        assertThat(result.blockedReason()).isEqualTo("ENVIO_DESABILITADO");
    }

    @Test
    void tokenAusenteBloqueiaChamadaReal() {
        WhatsAppProperties properties = properties(false, true, "", "12345", List.of("5511999998888"));
        OfficialCloudApiWhatsAppProvider provider = provider(properties);

        WhatsAppSendResult result = provider.sendText("5511999998888", "Olá");

        assertThat(result.status()).isEqualTo("SKIPPED");
        assertThat(result.blockedReason()).isEqualTo("TOKEN_AUSENTE");
    }

    @Test
    void allowlistBloqueiaNumeroNaoAutorizado() {
        WhatsAppProperties properties = properties(false, true, "token", "12345", List.of("5511777776666"));
        OfficialCloudApiWhatsAppProvider provider = provider(properties);

        WhatsAppSendResult result = provider.sendText("5511999998888", "Olá");

        assertThat(result.status()).isEqualTo("SKIPPED");
        assertThat(result.blockedReason()).isEqualTo("NUMERO_FORA_DA_ALLOWLIST");
    }

    private OfficialCloudApiWhatsAppProvider provider(WhatsAppProperties properties) {
        return new OfficialCloudApiWhatsAppProvider(
                properties,
                new WhatsAppTesterAllowlistService(properties),
                RestClient.builder());
    }

    private WhatsAppProperties properties(boolean dryRun, boolean sendEnabled, String token, String phoneNumberId, List<String> allowedNumbers) {
        WhatsAppProperties properties = new WhatsAppProperties();
        properties.setProvider("META_CLOUD");
        properties.setDryRun(dryRun);
        properties.setSendEnabled(sendEnabled);
        properties.setAccessToken(token);
        properties.setPhoneNumberId(phoneNumberId);
        properties.setAllowedTestNumbers(allowedNumbers);
        return properties;
    }
}
