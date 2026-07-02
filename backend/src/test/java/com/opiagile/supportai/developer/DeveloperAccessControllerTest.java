package com.opiagile.supportai.developer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.opiagile.supportai.common.ApiExceptionHandler;
import com.opiagile.supportai.security.SimpleRateLimiter;

class DeveloperAccessControllerTest {

    private final DeveloperAccessRequestRepository repository = org.mockito.Mockito.mock(DeveloperAccessRequestRepository.class);
    private final DeveloperAccessEmailTemplateService emailTemplateService =
            org.mockito.Mockito.mock(DeveloperAccessEmailTemplateService.class);

    @Test
    void deveRegistrarSolicitacaoValida() throws Exception {
        UUID requestId = UUID.fromString("00000000-0000-0000-0000-000000000123");
        when(repository.save(any(DeveloperAccessRequest.class), eq("203.0.113.10"), eq("JUnit")))
                .thenReturn(new DeveloperAccessRequestRecord(requestId, OffsetDateTime.parse("2026-06-27T12:00:00Z")));
        MockMvc mockMvc = mockMvc(new SimpleRateLimiter());

        mockMvc.perform(post("/api/developer-access-requests")
                        .header("X-Forwarded-For", "203.0.113.10")
                        .header("User-Agent", "JUnit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Maria Silva",
                                  "company": "Empresa Demo",
                                  "email": "maria@example.com",
                                  "useCase": "Validar chat RAG em portal de atendimento.",
                                  "requestedResources": "chat, documentos e fontes"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestId").value(requestId.toString()))
                .andExpect(jsonPath("$.status").value("NEW"));

        verify(repository).save(any(DeveloperAccessRequest.class), eq("203.0.113.10"), eq("JUnit"));
        verify(emailTemplateService).enqueueRequestReceived(any(DeveloperAccessRequestRecord.class), any(DeveloperAccessRequest.class));
    }

    @Test
    void deveValidarEmail() throws Exception {
        MockMvc mockMvc = mockMvc(new SimpleRateLimiter());

        mockMvc.perform(post("/api/developer-access-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Maria Silva",
                                  "email": "email-invalido",
                                  "useCase": "Validar API."
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("REQUISICAO_INVALIDA"));

        verify(repository, never()).save(any(), any(), any());
    }

    @Test
    void deveIgnorarHoneypotSemSalvar() throws Exception {
        MockMvc mockMvc = mockMvc(new SimpleRateLimiter());

        mockMvc.perform(post("/api/developer-access-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Bot",
                                  "email": "bot@example.com",
                                  "useCase": "Spam",
                                  "website": "https://spam.example"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        verify(repository, never()).save(any(), any(), any());
    }

    @Test
    void deveAplicarRateLimitPorIp() throws Exception {
        when(repository.save(any(DeveloperAccessRequest.class), eq("198.51.100.10"), any()))
                .thenReturn(new DeveloperAccessRequestRecord(UUID.randomUUID(), OffsetDateTime.parse("2026-06-27T12:00:00Z")));
        MockMvc mockMvc = mockMvc(new SimpleRateLimiter());

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/developer-access-requests")
                            .header("X-Forwarded-For", "198.51.100.10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validPayload("lead" + i + "@example.com")))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(post("/api/developer-access-requests")
                        .header("X-Forwarded-For", "198.51.100.10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("lead4@example.com")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("LIMITE_SOLICITACAO_EXCEDIDO"));
    }

    private MockMvc mockMvc(SimpleRateLimiter rateLimiter) {
        DeveloperAccessController controller = new DeveloperAccessController(repository, rateLimiter, emailTemplateService);
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler(Clock.systemUTC()))
                .build();
    }

    private String validPayload(String email) {
        return """
                {
                  "name": "Lead Demo",
                  "email": "%s",
                  "useCase": "Validar API."
                }
                """.formatted(email);
    }
}
