package com.opiagile.supportai.developer;

import java.time.OffsetDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.opiagile.supportai.common.ErrorResponse;
import com.opiagile.supportai.security.SimpleRateLimiter;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@Hidden
@Validated
@RestController
@RequestMapping("/api/developer-access-requests")
public class DeveloperAccessController {

    private static final int REQUESTS_PER_MINUTE = 3;

    private final DeveloperAccessRequestRepository repository;
    private final SimpleRateLimiter rateLimiter;
    private final DeveloperAccessEmailTemplateService emailTemplateService;

    public DeveloperAccessController(
            DeveloperAccessRequestRepository repository,
            SimpleRateLimiter rateLimiter,
            DeveloperAccessEmailTemplateService emailTemplateService) {
        this.repository = repository;
        this.rateLimiter = rateLimiter;
        this.emailTemplateService = emailTemplateService;
    }

    @PostMapping
    public ResponseEntity<?> requestAccess(
            @Valid @RequestBody DeveloperAccessRequest request,
            HttpServletRequest httpRequest) {
        if (request.website() != null && !request.website().isBlank()) {
            return ResponseEntity.ok(new DeveloperAccessResponse(
                    null,
                    "RECEIVED",
                    "Solicitação recebida.",
                    OffsetDateTime.now()));
        }
        String clientIp = clientIp(httpRequest);
        if (!rateLimiter.allow("developer-access:" + clientIp, REQUESTS_PER_MINUTE)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErrorResponse(
                            "LIMITE_SOLICITACAO_EXCEDIDO",
                            "Limite de solicitações excedido. Aguarde um minuto e tente novamente.",
                            OffsetDateTime.now()));
        }

        DeveloperAccessRequestRecord record = repository.save(request, clientIp, httpRequest.getHeader("User-Agent"));
        emailTemplateService.enqueueRequestReceived(record, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new DeveloperAccessResponse(
                        record.id(),
                        "NEW",
                        "Solicitação registrada. A Opiagile entrará em contato pelo email informado.",
                        record.createdAt()));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return truncate(forwardedFor.split(",")[0].trim(), 80);
        }
        return truncate(request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr(), 80);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
