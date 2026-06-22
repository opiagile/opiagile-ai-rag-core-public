package com.opiagile.supportai.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.opiagile.supportai.chat.ChatService;

class WhatsappWebhookControllerTest {

    @Test
    void deveValidarWebhookMetaComTokenCorreto() {
        WhatsAppProperties properties = new WhatsAppProperties();
        properties.setVerifyToken("token-local");
        WhatsappWebhookController controller = new WhatsappWebhookController(
                mock(ChatService.class),
                mock(WhatsAppProvider.class),
                properties,
                new WhatsAppTesterAllowlistService(properties),
                mock(WhatsAppMetaWebhookService.class));

        ResponseEntity<String> response = controller.verifyMetaWebhook("subscribe", "token-local", "12345");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("12345");
    }

    @Test
    void deveRecusarWebhookMetaComTokenErrado() {
        WhatsAppProperties properties = new WhatsAppProperties();
        properties.setVerifyToken("token-local");
        WhatsappWebhookController controller = new WhatsappWebhookController(
                mock(ChatService.class),
                mock(WhatsAppProvider.class),
                properties,
                new WhatsAppTesterAllowlistService(properties),
                mock(WhatsAppMetaWebhookService.class));

        ResponseEntity<String> response = controller.verifyMetaWebhook("subscribe", "errado", "12345");

        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }
    @Test
    void deveExporStatusSemSegredos() {
        WhatsAppProperties properties = new WhatsAppProperties();
        properties.setProvider("META_CLOUD");
        properties.setDryRun(true);
        properties.setSendEnabled(false);
        properties.setSignatureRequired(true);
        properties.setVerifyToken("token-local");
        properties.setAppSecret("segredo-local");
        properties.setAccessToken("token-acesso-local");
        properties.setPhoneNumberId("12345");
        properties.setAllowedTestNumbers(List.of("5511999998888"));
        properties.setBlockUnauthorized(true);
        WhatsappWebhookController controller = new WhatsappWebhookController(
                mock(ChatService.class),
                mock(WhatsAppProvider.class),
                properties,
                new WhatsAppTesterAllowlistService(properties),
                mock(WhatsAppMetaWebhookService.class));

        WhatsAppStatusResponse response = controller.status();

        assertThat(response.provider()).isEqualTo("META_CLOUD");
        assertThat(response.accessTokenConfigured()).isTrue();
        assertThat(response.appSecretConfigured()).isTrue();
        assertThat(response.verifyTokenConfigured()).isTrue();
        assertThat(response.allowedTestNumbersCount()).isEqualTo(1);
        assertThat(response.blockUnauthorized()).isTrue();
    }

}
