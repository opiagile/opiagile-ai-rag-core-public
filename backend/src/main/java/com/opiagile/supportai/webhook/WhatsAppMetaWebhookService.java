package com.opiagile.supportai.webhook;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.opiagile.supportai.chat.ChatRequest;
import com.opiagile.supportai.chat.ChatResponse;
import com.opiagile.supportai.chat.ChatService;
import com.opiagile.supportai.tenant.TenantContextResolver;

@Service
public class WhatsAppMetaWebhookService {

    private final WhatsAppProperties properties;
    private final WhatsAppSignatureVerifier signatureVerifier;
    private final WhatsAppMetaPayloadParser payloadParser;
    private final WhatsAppTesterAllowlistService allowlistService;
    private final WhatsAppRateLimiter rateLimiter;
    private final ChatService chatService;
    private final DefaultWhatsAppOutboundService outboundService;
    private final WhatsAppWebhookEventRepository eventRepository;
    private final TenantContextResolver tenantContextResolver;
    private final String environment;

    public WhatsAppMetaWebhookService(
            WhatsAppProperties properties,
            WhatsAppSignatureVerifier signatureVerifier,
            WhatsAppMetaPayloadParser payloadParser,
            WhatsAppTesterAllowlistService allowlistService,
            WhatsAppRateLimiter rateLimiter,
            ChatService chatService,
            DefaultWhatsAppOutboundService outboundService,
            WhatsAppWebhookEventRepository eventRepository,
            TenantContextResolver tenantContextResolver,
            @Value("${app.environment:local}") String environment) {
        this.properties = properties;
        this.signatureVerifier = signatureVerifier;
        this.payloadParser = payloadParser;
        this.allowlistService = allowlistService;
        this.rateLimiter = rateLimiter;
        this.chatService = chatService;
        this.outboundService = outboundService;
        this.eventRepository = eventRepository;
        this.tenantContextResolver = tenantContextResolver;
        this.environment = environment;
    }

    public WhatsAppMetaWebhookResponse process(byte[] rawBody, String signatureHeader) {
        validateSignature(rawBody, signatureHeader);
        List<WhatsAppInboundMessage> events = payloadParser.parse(rawBody);
        if (events.isEmpty()) {
            eventRepository.save(provider(), null, null, null, "empty", false, "SEM_EVENTOS", properties.isDryRun(), properties.isSendEnabled(), "SKIPPED");
            return accepted(false, "SEM_EVENTOS", null, null, "SKIPPED");
        }
        WhatsAppMetaWebhookResponse lastResponse = null;
        for (WhatsAppInboundMessage event : events) {
            lastResponse = processEvent(event);
        }
        return lastResponse;
    }

    private WhatsAppMetaWebhookResponse processEvent(WhatsAppInboundMessage event) {
        String maskedPhone = PhoneNumberMasker.mask(event.from());
        if (!event.isTextMessage()) {
            String reason = ignoredReason(event);
            eventRepository.save(provider(), event.messageId(), null, maskedPhone, event.eventType(), false, reason, properties.isDryRun(), properties.isSendEnabled(), "SKIPPED");
            return accepted(false, reason, maskedPhone, null, "SKIPPED");
        }
        if (!allowlistService.isAllowed(event.from())) {
            String reason = "NUMERO_FORA_DA_ALLOWLIST";
            eventRepository.save(provider(), event.messageId(), null, maskedPhone, event.eventType(), false, reason, properties.isDryRun(), properties.isSendEnabled(), "BLOCKED");
            return accepted(false, reason, maskedPhone, null, "BLOCKED");
        }
        if (!rateLimiter.allow(event.from())) {
            String reason = "RATE_LIMIT_EXCEDIDO";
            eventRepository.save(provider(), event.messageId(), null, maskedPhone, event.eventType(), false, reason, properties.isDryRun(), properties.isSendEnabled(), "BLOCKED");
            return accepted(false, reason, maskedPhone, null, "BLOCKED");
        }
        ChatResponse chatResponse = chatService.answer(
                tenantContextResolver.resolve(null, null),
                new ChatRequest(
                        null,
                        messageWithName(event),
                        "WHATSAPP",
                        PhoneNumberMasker.normalize(event.from())));
        WhatsAppSendResult sendResult = outboundService.send(event.from(), chatResponse.answer());
        eventRepository.save(provider(), event.messageId(), chatResponse.conversationId(), maskedPhone, event.eventType(), true,
                sendResult.blockedReason(), properties.isDryRun(), properties.isSendEnabled(), sendResult.status());
        return new WhatsAppMetaWebhookResponse(
                provider(),
                "ACCEPTED",
                true,
                properties.isDryRun(),
                properties.isSendEnabled(),
                sendResult.blockedReason(),
                maskedPhone,
                chatResponse.conversationId(),
                sendResult.status());
    }

    private void validateSignature(byte[] rawBody, String signatureHeader) {
        if (!properties.isSignatureRequired()) {
            if (isLocalEnvironment()) {
                return;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Assinatura obrigatória fora de ambiente local.");
        }
        if (!signatureVerifier.verify(rawBody, signatureHeader)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Assinatura WhatsApp inválida.");
        }
    }

    private boolean isLocalEnvironment() {
        String normalized = environment == null ? "" : environment.trim().toLowerCase();
        return normalized.equals("local") || normalized.equals("dev") || normalized.equals("test") || normalized.equals("github-actions");
    }

    private String messageWithName(WhatsAppInboundMessage event) {
        if (event.contactName() == null || event.contactName().isBlank()) {
            return event.text();
        }
        return "Sou " + event.contactName().trim() + ". " + event.text();
    }

    private String ignoredReason(WhatsAppInboundMessage event) {
        if ("status".equals(event.eventType())) {
            return "STATUS_IGNORADO";
        }
        return "TIPO_NAO_SUPORTADO_" + (event.messageType() == null ? "DESCONHECIDO" : event.messageType().toUpperCase());
    }

    private WhatsAppMetaWebhookResponse accepted(boolean processed, String reason, String maskedPhone, java.util.UUID conversationId, String outboundStatus) {
        return new WhatsAppMetaWebhookResponse(provider(), "ACCEPTED", processed, properties.isDryRun(), properties.isSendEnabled(), reason, maskedPhone, conversationId, outboundStatus);
    }

    private String provider() {
        return properties.isMetaCloud() ? "META_CLOUD" : properties.getProvider();
    }
}
