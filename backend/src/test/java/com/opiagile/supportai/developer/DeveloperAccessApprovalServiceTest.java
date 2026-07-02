package com.opiagile.supportai.developer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import com.opiagile.supportai.security.ApiClientRepository;
import com.opiagile.supportai.tenant.TenantContext;
import com.opiagile.supportai.tenant.TenantRepository;

class DeveloperAccessApprovalServiceTest {

    private final DeveloperAccessRequestRepository accessRequestRepository = mock(DeveloperAccessRequestRepository.class);
    private final ApiClientRepository apiClientRepository = mock(ApiClientRepository.class);
    private final TenantRepository tenantRepository = mock(TenantRepository.class);
    private final DeveloperAccessKeyDeliveryService keyDeliveryService = mock(DeveloperAccessKeyDeliveryService.class);
    private final DeveloperAccessEmailTemplateService emailTemplateService = mock(DeveloperAccessEmailTemplateService.class);
    private final DeveloperAccessApprovalService service =
            new DeveloperAccessApprovalService(
                    accessRequestRepository,
                    apiClientRepository,
                    tenantRepository,
                    keyDeliveryService,
                    emailTemplateService);

    @Test
    void deveGerarApiKeySomenteNaRespostaEGravarHash() {
        UUID requestId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        TenantContext tenantContext = new TenantContext(UUID.randomUUID(), "demo", UUID.randomUUID(), "clinica-demo", "Clínica Demo");
        when(accessRequestRepository.findById(requestId)).thenReturn(Optional.of(request(requestId, "NEW")));
        when(tenantRepository.findContext("demo", "clinica-demo")).thenReturn(Optional.of(tenantContext));
        when(apiClientRepository.create(eq(tenantContext), anyString(), anyString(), anyString(), any(), anyInt()))
                .thenReturn(clientId);
        when(accessRequestRepository.markApproved(eq(requestId), eq(clientId), eq("demo"), eq("clinica-demo"), eq(null))).thenReturn(true);
        when(keyDeliveryService.createLink(eq(requestId), eq(clientId), anyString(), anyString(), eq("demo"), eq("clinica-demo"), any(), eq(25), eq(null), eq(null)))
                .thenReturn(new DeveloperAccessKeyDeliveryLink(
                        UUID.randomUUID(),
                        "token-entrega",
                        "https://opiagile.com/developers/access-key/token-entrega",
                        OffsetDateTime.parse("2026-07-01T12:00:00Z")));

        DeveloperAccessApprovalResponse response = service.approve(requestId, new DeveloperAccessApprovalRequest(
                "demo",
                "clinica-demo",
                Set.of("chat:write", "providers:read"),
                25,
                "Sandbox Empresa Demo",
                null));

        assertThat(response.apiClientId()).isEqualTo(clientId);
        assertThat(response.apiKey()).startsWith("opiagile_sandbox_");
        assertThat(response.keyPrefix()).isEqualTo(response.apiKey().substring(0, 24));
        assertThat(response.scopes()).containsExactlyInAnyOrder("chat:write", "providers:read");
        assertThat(response.rateLimitPerMinute()).isEqualTo(25);
        assertThat(response.keyDeliveryUrl()).isEqualTo("https://opiagile.com/developers/access-key/token-entrega");
        assertThat(response.keyDeliveryExpiresAt()).isEqualTo(OffsetDateTime.parse("2026-07-01T12:00:00Z"));

        ArgumentCaptor<String> keyHash = ArgumentCaptor.forClass(String.class);
        verify(apiClientRepository).create(eq(tenantContext), eq("Sandbox Empresa Demo"), eq(response.keyPrefix()), keyHash.capture(), any(), eq(25));
        assertThat(keyHash.getValue()).hasSize(64);
        assertThat(keyHash.getValue()).doesNotContain(response.apiKey());
        verify(accessRequestRepository).markApproved(requestId, clientId, "demo", "clinica-demo", null);
        verify(emailTemplateService).enqueueSandboxApproved(any(), eq(response), eq(response.keyDeliveryUrl()), eq(response.keyDeliveryExpiresAt()));
    }

    @Test
    void deveCriarSandboxTemporarioComExpiracaoEAvisoLgpd() {
        UUID requestId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        TenantContext tenantContext = new TenantContext(
                UUID.randomUUID(),
                "sandbox-empresa-demo-" + requestId.toString().substring(0, 8),
                UUID.randomUUID(),
                "empresa-demo-sandbox-" + requestId.toString().substring(0, 8),
                "Empresa Demo - Sandbox");
        when(accessRequestRepository.findById(requestId)).thenReturn(Optional.of(request(requestId, "NEW")));
        when(tenantRepository.createTemporarySandbox(
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        any()))
                .thenReturn(tenantContext);
        when(apiClientRepository.create(eq(tenantContext), anyString(), anyString(), anyString(), any(), eq(30), any()))
                .thenReturn(clientId);
        when(accessRequestRepository.markApproved(eq(requestId), eq(clientId), eq(tenantContext.tenantSlug()), eq(tenantContext.workspaceSlug()), any()))
                .thenReturn(true);
        when(keyDeliveryService.createLink(
                        eq(requestId),
                        eq(clientId),
                        anyString(),
                        anyString(),
                        eq(tenantContext.tenantSlug()),
                        eq(tenantContext.workspaceSlug()),
                        any(),
                        eq(30),
                        any(),
                        anyString()))
                .thenReturn(new DeveloperAccessKeyDeliveryLink(
                        UUID.randomUUID(),
                        "token-entrega",
                        "https://opiagile.com/developers/access-key/token-entrega",
                        OffsetDateTime.parse("2026-07-01T12:00:00Z")));

        DeveloperAccessApprovalResponse response = service.approveTemporarySandbox(requestId, new DeveloperAccessApprovalRequest(
                null,
                null,
                Set.of("chat:write"),
                30,
                null,
                24));

        assertThat(response.apiKey()).startsWith("opiagile_sandbox_");
        assertThat(response.tenantSlug()).startsWith("sandbox-empresa-demo-");
        assertThat(response.workspaceSlug()).startsWith("empresa-demo-sandbox-");
        assertThat(response.expiresAt()).isNotNull();
        assertThat(response.retentionNotice()).contains("tenant/workspace", "LGPD");
    }

    @Test
    void deveBloquearReemissaoQuandoSolicitacaoJaEstaAprovada() {
        UUID requestId = UUID.randomUUID();
        when(accessRequestRepository.findById(requestId)).thenReturn(Optional.of(request(requestId, "APPROVED")));

        assertThatThrownBy(() -> service.approve(requestId, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("já aprovada");
    }

    private DeveloperAccessRequestAdminResponse request(UUID requestId, String status) {
        return new DeveloperAccessRequestAdminResponse(
                requestId,
                OffsetDateTime.parse("2026-06-30T12:00:00Z"),
                OffsetDateTime.parse("2026-06-30T12:00:00Z"),
                "Ana",
                "Empresa Demo",
                "ana@example.com",
                "Testar API",
                "Chat e documentos",
                "developers-portal",
                status,
                true,
                1,
                null,
                null,
                null,
                null,
                null);
    }
}
