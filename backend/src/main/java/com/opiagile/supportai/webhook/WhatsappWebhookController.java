package com.opiagile.supportai.webhook;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.opiagile.supportai.chat.ChatRequest;
import com.opiagile.supportai.chat.ChatResponse;
import com.opiagile.supportai.chat.ChatService;
import com.opiagile.supportai.tenant.TenantContextResolver;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/webhooks/whatsapp")
public class WhatsappWebhookController {

    private final ChatService chatService;
    private final WhatsAppProvider whatsAppProvider;
    private final WhatsAppProperties properties;
    private final WhatsAppTesterAllowlistService allowlistService;
    private final WhatsAppMetaWebhookService metaWebhookService;
    private final TenantContextResolver tenantContextResolver;

    public WhatsappWebhookController(
            ChatService chatService,
            WhatsAppProvider whatsAppProvider,
            WhatsAppProperties properties,
            WhatsAppTesterAllowlistService allowlistService,
            WhatsAppMetaWebhookService metaWebhookService,
            TenantContextResolver tenantContextResolver) {
        this.chatService = chatService;
        this.whatsAppProvider = whatsAppProvider;
        this.properties = properties;
        this.allowlistService = allowlistService;
        this.metaWebhookService = metaWebhookService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping
    public WhatsappWebhookResponse receive(@Valid @RequestBody WhatsappWebhookRequest request) {
        ChatResponse chatResponse = chatService.answer(
                tenantContextResolver.resolve(null, null),
                new ChatRequest(
                        null,
                        messageWithName(request),
                        "WHATSAPP",
                        request.from()));
        whatsAppProvider.sendMessage(request.from(), chatResponse.answer());
        return new WhatsappWebhookResponse(
                whatsAppProvider.providerName(),
                request.from(),
                chatResponse.conversationId(),
                chatResponse.answer(),
                chatResponse.intent(),
                chatResponse.handoffRequired(),
                chatResponse.leadStatus());
    }

    @GetMapping(value = "/meta", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verifyMetaWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {
        if ("subscribe".equals(mode)
                && challenge != null
                && !challenge.isBlank()
                && properties.hasVerifyToken()
                && properties.getVerifyToken().equals(verifyToken)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("verification failed");
    }

    @PostMapping(value = "/meta", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WhatsAppMetaWebhookResponse> receiveMetaWebhook(
            @RequestBody byte[] rawBody,
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String signatureHeader) {
        return ResponseEntity.ok(metaWebhookService.process(rawBody, signatureHeader));
    }

    @GetMapping("/status")
    public WhatsAppStatusResponse status() {
        return WhatsAppStatusResponse.from(properties, allowlistService);
    }

    private String messageWithName(WhatsappWebhookRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            return request.message();
        }
        return "Sou " + request.name().trim() + ". " + request.message();
    }
}
