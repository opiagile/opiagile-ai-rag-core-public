package com.opiagile.supportai.chat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.opiagile.supportai.common.ErrorResponse;
import com.opiagile.supportai.security.SimpleRateLimiter;
import com.opiagile.supportai.tenant.TenantContext;
import com.opiagile.supportai.tenant.TenantContextResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/site-chat")
public class SiteChatController {

    private static final String API_KEY_HEADER = "X-OPIAGILE-API-KEY";

    private final ChatService chatService;
    private final TenantContextResolver tenantContextResolver;
    private final SiteChatProperties properties;
    private final SimpleRateLimiter rateLimiter;

    public SiteChatController(
            ChatService chatService,
            TenantContextResolver tenantContextResolver,
            SiteChatProperties properties,
            SimpleRateLimiter rateLimiter) {
        this.chatService = chatService;
        this.tenantContextResolver = tenantContextResolver;
        this.properties = properties;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping
    public ResponseEntity<?> chat(
            @RequestHeader(name = API_KEY_HEADER, required = false) String apiKey,
            @Valid @RequestBody SiteChatRequest request,
            HttpServletRequest httpRequest) {
        if (!properties.enabled()) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, "SITE_CHAT_DESABILITADO", "Chat da landing indisponível no momento.");
        }
        if (!properties.hasApiKey() || !apiKeyMatches(apiKey)) {
            return error(HttpStatus.UNAUTHORIZED, "SITE_CHAT_NAO_AUTORIZADO", "Chave de acesso ausente ou inválida.");
        }
        if (!rateLimiter.allow("site-chat:" + clientIp(httpRequest), properties.rateLimitPerMinute())) {
            return error(HttpStatus.TOO_MANY_REQUESTS, "SITE_CHAT_LIMITE_EXCEDIDO", "Limite de mensagens excedido. Aguarde um minuto e tente novamente.");
        }

        TenantContext tenantContext = tenantContextResolver.resolve(properties.tenant(), properties.workspace());
        ChatResponse response = chatService.answer(
                tenantContext,
                new ChatRequest(
                        request.conversationId(),
                        request.message(),
                        "SITE",
                        normalizeVisitorId(request.visitorId()),
                        request.responseLanguage()));
        return ResponseEntity.ok(new SiteChatResponse(
                response.conversationId(),
                response.answer(),
                response.handoffRequired(),
                response.leadStatus(),
                response.responseMode(),
                response.model()));
    }

    private boolean apiKeyMatches(String providedApiKey) {
        byte[] expected = properties.apiKey().getBytes(StandardCharsets.UTF_8);
        byte[] provided = normalize(providedApiKey).getBytes(StandardCharsets.UTF_8);
        return expected.length == provided.length && MessageDigest.isEqual(expected, provided);
    }

    private String normalizeVisitorId(String visitorId) {
        if (visitorId == null || visitorId.isBlank()) {
            return "opiagile-site";
        }
        return visitorId.trim();
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, message, OffsetDateTime.now()));
    }
}
