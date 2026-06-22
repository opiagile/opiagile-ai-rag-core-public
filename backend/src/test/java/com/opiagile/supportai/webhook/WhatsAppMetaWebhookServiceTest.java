package com.opiagile.supportai.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opiagile.supportai.chat.ChatRequest;
import com.opiagile.supportai.chat.ChatResponse;
import com.opiagile.supportai.chat.ChatService;

class WhatsAppMetaWebhookServiceTest {

    @Test
    void deveProcessarTextoPermitidoEmDryRun() {
        Fixture fixture = fixture(true, false);
        UUID conversationId = UUID.randomUUID();
        when(fixture.chatService.answer(any(ChatRequest.class))).thenReturn(new ChatResponse(
                conversationId,
                "Resposta de teste",
                "AGENDAR",
                List.of(),
                false,
                "QUALIFIED",
                12,
                "LLM",
                "OPENAI",
                "gpt-5-mini",
                null,
                120L,
                null,
                null,
                null));
        when(fixture.outboundService.send("5511999998888", "Resposta de teste")).thenReturn(WhatsAppSendResult.dryRunResult());

        WhatsAppMetaWebhookResponse response = fixture.service.process(textPayload(), null);

        assertThat(response.processed()).isTrue();
        assertThat(response.conversationId()).isEqualTo(conversationId);
        assertThat(response.outboundStatus()).isEqualTo("DRY_RUN");
        verify(fixture.chatService).answer(any(ChatRequest.class));
    }

    @Test
    void naoDeveChamarChatQuandoNumeroNaoAutorizado() {
        Fixture fixture = fixture(false, false);

        WhatsAppMetaWebhookResponse response = fixture.service.process(textPayload(), null);

        assertThat(response.processed()).isFalse();
        assertThat(response.blockedReason()).isEqualTo("NUMERO_FORA_DA_ALLOWLIST");
        verify(fixture.chatService, never()).answer(any(ChatRequest.class));
    }

    @Test
    void naoDeveChamarChatQuandoEventoNaoForTexto() {
        Fixture fixture = fixture(true, false);

        WhatsAppMetaWebhookResponse response = fixture.service.process(statusPayload(), null);

        assertThat(response.processed()).isFalse();
        assertThat(response.blockedReason()).isEqualTo("STATUS_IGNORADO");
        verify(fixture.chatService, never()).answer(any(ChatRequest.class));
    }

    @Test
    void deveBloquearAssinaturaAusenteQuandoObrigatoria() {
        Fixture fixture = fixture(true, false);
        fixture.properties.setSignatureRequired(true);
        fixture.properties.setAppSecret("segredo");

        org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class, () -> fixture.service.process(textPayload(), null));
        verify(fixture.chatService, never()).answer(any(ChatRequest.class));
    }

    private Fixture fixture(boolean allowed, boolean signatureRequired) {
        WhatsAppProperties properties = new WhatsAppProperties();
        properties.setProvider("META_CLOUD");
        properties.setDryRun(true);
        properties.setSendEnabled(false);
        properties.setSignatureRequired(signatureRequired);
        properties.setAllowedTestNumbers(allowed ? List.of("5511999998888") : List.of("5511777776666"));
        ChatService chatService = mock(ChatService.class);
        DefaultWhatsAppOutboundService outboundService = mock(DefaultWhatsAppOutboundService.class);
        WhatsAppWebhookEventRepository repository = mock(WhatsAppWebhookEventRepository.class);
        WhatsAppMetaWebhookService service = new WhatsAppMetaWebhookService(
                properties,
                new WhatsAppSignatureVerifier(properties),
                new WhatsAppMetaPayloadParser(new ObjectMapper()),
                new WhatsAppTesterAllowlistService(properties),
                new WhatsAppRateLimiter(properties),
                chatService,
                outboundService,
                repository,
                "local");
        return new Fixture(properties, chatService, outboundService, service);
    }

    private byte[] textPayload() {
        return WhatsAppMetaPayloadParserTest.textPayload().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] statusPayload() {
        return """
                {"entry":[{"changes":[{"value":{"metadata":{"phone_number_id":"12345"},"statuses":[{"id":"wamid.TESTE","status":"sent"}]}}]}]}
                """.getBytes(StandardCharsets.UTF_8);
    }

    private record Fixture(
            WhatsAppProperties properties,
            ChatService chatService,
            DefaultWhatsAppOutboundService outboundService,
            WhatsAppMetaWebhookService service) {
    }
}
