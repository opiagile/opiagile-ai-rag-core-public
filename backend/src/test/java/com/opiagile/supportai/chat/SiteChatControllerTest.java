package com.opiagile.supportai.chat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import tools.jackson.databind.ObjectMapper;
import com.opiagile.supportai.common.ApiExceptionHandler;
import com.opiagile.supportai.security.SimpleRateLimiter;
import com.opiagile.supportai.tenant.TenantContext;
import com.opiagile.supportai.tenant.TenantContextResolver;

class SiteChatControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChatService chatService = org.mockito.Mockito.mock(ChatService.class);
    private final TenantContextResolver tenantContextResolver = org.mockito.Mockito.mock(TenantContextResolver.class);
    private final TenantContext tenantContext = new TenantContext(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            "opiagile",
            UUID.fromString("00000000-0000-0000-0000-000000000002"),
            "opiagile-rag",
            "Assistente Opiagile");

    @Test
    void deveBloquearSemChave() throws Exception {
        MockMvc mockMvc = mockMvc(properties("chave-site", 20));

        mockMvc.perform(post("/api/site-chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"How can Opiagile help my company?","visitorId":"lead-1","responseLanguage":"EN"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("SITE_CHAT_NAO_AUTORIZADO"));

        verifyNoInteractions(chatService);
    }

    @Test
    void deveChamarChatServiceComTenantDaOpiagile() throws Exception {
        UUID conversationId = UUID.fromString("00000000-0000-0000-0000-000000000100");
        when(tenantContextResolver.resolve("opiagile", "opiagile-rag")).thenReturn(tenantContext);
        when(chatService.answer(eq(tenantContext), any(ChatRequest.class))).thenReturn(new ChatResponse(
                conversationId,
                "A Opiagile ajuda sua empresa a responder dúvidas com base nos materiais conectados.",
                "DUVIDA_FAQ",
                List.of(),
                false,
                "QUALIFYING",
                20,
                "DEMO",
                "DEMO",
                "demo-local",
                null,
                0L,
                null,
                null,
                null));
        MockMvc mockMvc = mockMvc(properties("chave-site", 20));

        mockMvc.perform(post("/api/site-chat")
                        .header("X-OPIAGILE-API-KEY", "chave-site")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"How can Opiagile help my company?","visitorId":"lead-1","responseLanguage":"EN"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(conversationId.toString()))
                .andExpect(jsonPath("$.answer").value("A Opiagile ajuda sua empresa a responder dúvidas com base nos materiais conectados."));

        ArgumentCaptor<ChatRequest> requestCaptor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(chatService).answer(eq(tenantContext), requestCaptor.capture());
        ChatRequest request = requestCaptor.getValue();
        org.assertj.core.api.Assertions.assertThat(request.channel()).isEqualTo("SITE");
        org.assertj.core.api.Assertions.assertThat(request.contactId()).isEqualTo("lead-1");
        org.assertj.core.api.Assertions.assertThat(request.responseLanguage()).isEqualTo("EN");
    }

    @Test
    void deveAplicarRateLimit() throws Exception {
        when(tenantContextResolver.resolve("opiagile", "opiagile-rag")).thenReturn(tenantContext);
        when(chatService.answer(eq(tenantContext), any(ChatRequest.class))).thenReturn(new ChatResponse(
                UUID.randomUUID(),
                "Resposta",
                "DUVIDA_FAQ",
                List.of(),
                false,
                "QUALIFYING",
                20,
                "DEMO",
                "DEMO",
                "demo-local",
                null,
                0L,
                null,
                null,
                null));
        MockMvc mockMvc = mockMvc(properties("chave-site", 1));

        mockMvc.perform(post("/api/site-chat")
                        .header("X-OPIAGILE-API-KEY", "chave-site")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Primeira pergunta","visitorId":"lead-1"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/site-chat")
                        .header("X-OPIAGILE-API-KEY", "chave-site")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Segunda pergunta","visitorId":"lead-1"}
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("SITE_CHAT_LIMITE_EXCEDIDO"));
    }

    private MockMvc mockMvc(SiteChatProperties properties) {
        SiteChatController controller = new SiteChatController(
                chatService,
                tenantContextResolver,
                properties,
                new SimpleRateLimiter());
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler(java.time.Clock.systemUTC()))
                .build();
    }

    private SiteChatProperties properties(String apiKey, int rateLimitPerMinute) {
        return new SiteChatProperties(true, apiKey, "opiagile", "opiagile-rag", rateLimitPerMinute);
    }
}
