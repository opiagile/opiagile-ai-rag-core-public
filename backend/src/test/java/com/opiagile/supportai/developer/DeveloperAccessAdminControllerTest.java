package com.opiagile.supportai.developer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.opiagile.supportai.security.DemoSecurityPolicy;
import com.opiagile.supportai.tenant.TenantRepository;
import com.opiagile.supportai.tenant.WorkspaceResponse;

class DeveloperAccessAdminControllerTest {

    private final DeveloperAccessRequestRepository repository = mock(DeveloperAccessRequestRepository.class);
    private final DeveloperAccessApprovalService approvalService = mock(DeveloperAccessApprovalService.class);
    private final TenantRepository tenantRepository = mock(TenantRepository.class);
    private final DemoSecurityPolicy securityPolicy = new DemoSecurityPolicy("", "admin-token", true, 10, 10);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new DeveloperAccessAdminController(repository, approvalService, securityPolicy, tenantRepository))
            .build();

    @Test
    void deveBloquearListagemSemTokenAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/developer-access-requests"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveListarSolicitacoesComTokenAdmin() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(repository.findForAdmin("NEW", 50)).thenReturn(List.of(new DeveloperAccessRequestAdminResponse(
                requestId,
                OffsetDateTime.parse("2026-06-30T12:00:00Z"),
                OffsetDateTime.parse("2026-06-30T12:00:00Z"),
                "Ana",
                "Empresa Demo",
                "ana@example.com",
                "Testar API",
                "Chat e documentos",
                "developers-portal",
                "NEW",
                false,
                0,
                null,
                null,
                null,
                null,
                null)));

        mockMvc.perform(get("/api/admin/developer-access-requests")
                        .header("X-Demo-Admin-Token", "admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(requestId.toString()))
                .andExpect(jsonPath("$[0].email").value("ana@example.com"));
    }

    @Test
    void deveAprovarSolicitacaoComTokenAdmin() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID apiClientId = UUID.randomUUID();
        when(approvalService.approve(eq(requestId), any()))
                .thenReturn(new DeveloperAccessApprovalResponse(
                        requestId,
                        apiClientId,
                        "opiagile_sandbox_token",
                        "opiagile_sandbox_token".substring(0, 20),
                        "demo",
                        "clinica-demo",
                        Set.of("chat:write"),
                        30,
                        OffsetDateTime.parse("2026-06-30T12:00:00Z"),
                        null,
                        "https://opiagile.com/developers/access-key/token-entrega",
                        OffsetDateTime.parse("2026-07-01T12:00:00Z"),
                        null,
                        "Copie esta API key agora."));

        mockMvc.perform(post("/api/admin/developer-access-requests/{requestId}/approve", requestId)
                        .header("X-Demo-Admin-Token", "admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantSlug": "demo",
                                  "workspaceSlug": "clinica-demo",
                                  "scopes": ["chat:write"],
                                  "rateLimitPerMinute": 30
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiClientId").value(apiClientId.toString()))
                .andExpect(jsonPath("$.apiKey").value("opiagile_sandbox_token"));

        verify(approvalService).approve(eq(requestId), any());
    }

    @Test
    void deveAprovarSandboxTemporarioComTokenAdmin() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID apiClientId = UUID.randomUUID();
        when(approvalService.approveTemporarySandbox(eq(requestId), any()))
                .thenReturn(new DeveloperAccessApprovalResponse(
                        requestId,
                        apiClientId,
                        "opiagile_sandbox_token",
                        "opiagile_sandbox_token".substring(0, 20),
                        "sandbox-empresa-demo",
                        "empresa-demo-sandbox",
                        Set.of("chat:write"),
                        30,
                        OffsetDateTime.parse("2026-06-30T12:00:00Z"),
                        OffsetDateTime.parse("2026-07-01T12:00:00Z"),
                        "https://opiagile.com/developers/access-key/token-entrega",
                        OffsetDateTime.parse("2026-07-01T12:00:00Z"),
                        "Sandbox temporário com retenção LGPD.",
                        "Copie esta API key agora."));

        mockMvc.perform(post("/api/admin/developer-access-requests/{requestId}/approve-temporary-sandbox", requestId)
                        .header("X-Demo-Admin-Token", "admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scopes": ["chat:write"],
                                  "rateLimitPerMinute": 30,
                                  "expiresInHours": 24
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.apiClientId").value(apiClientId.toString()))
                .andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.retentionNotice").value("Sandbox temporário com retenção LGPD."));

        verify(approvalService).approveTemporarySandbox(eq(requestId), any());
    }

    @Test
    void deveListarWorkspacesComTokenAdmin() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        when(tenantRepository.findWorkspaces("demo")).thenReturn(List.of(new WorkspaceResponse(
                workspaceId,
                "demo",
                "clinica-demo",
                "Clínica Demo",
                "Workspace de demonstração")));

        mockMvc.perform(get("/api/admin/developer-access-requests/workspaces")
                        .header("X-Demo-Admin-Token", "admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tenantSlug").value("demo"))
                .andExpect(jsonPath("$[0].workspaceSlug").value("clinica-demo"))
                .andExpect(jsonPath("$[0].workspaceName").value("Clínica Demo"));
    }
}
