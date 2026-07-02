package com.opiagile.supportai.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import tools.jackson.databind.ObjectMapper;
import com.opiagile.supportai.tenant.TenantContext;

class ApiClientAuthenticationFilterTest {

    private final ApiClientRepository repository = mock(ApiClientRepository.class);
    private final ApiClientUsageLogRepository usageLogRepository = mock(ApiClientUsageLogRepository.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void devePermitirFluxoLegadoSemChaveQuandoNaoObrigatoria() throws Exception {
        ApiClientAuthenticationFilter filter = filter(false);
        MockHttpServletRequest request = post("/api/chat");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        verify(repository, never()).findByKeyHash(anyString());
    }

    @Test
    void deveBloquearEndpointProtegidoQuandoChaveForObrigatoriaEAusente() throws Exception {
        ApiClientAuthenticationFilter filter = filter(true);
        MockHttpServletRequest request = post("/api/chat");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentAsString()).contains("API_KEY_AUSENTE");
    }

    @Test
    void deveBloquearChaveInvalida() throws Exception {
        when(repository.findByKeyHash(ApiKeyHasher.sha256Hex("opg_teste"))).thenReturn(Optional.empty());
        ApiClientAuthenticationFilter filter = filter(false);
        MockHttpServletRequest request = post("/api/chat");
        request.addHeader("X-OPIAGILE-API-KEY", "opg_teste");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentAsString()).contains("API_KEY_INVALIDA");
    }

    @Test
    void deveBloquearEscopoInsuficiente() throws Exception {
        when(repository.findByKeyHash(ApiKeyHasher.sha256Hex("opg_teste"))).thenReturn(Optional.of(client(Set.of("documents:read"))));
        ApiClientAuthenticationFilter filter = filter(false);
        MockHttpServletRequest request = post("/api/chat");
        request.addHeader("X-OPIAGILE-API-KEY", "opg_teste");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getContentAsString()).contains("ESCOPO_INSUFICIENTE");
    }

    @Test
    void devePermitirChaveValidaComEscopoCorreto() throws Exception {
        ApiClientRecord client = client(Set.of("chat:write"));
        when(repository.findByKeyHash(ApiKeyHasher.sha256Hex("opg_teste"))).thenReturn(Optional.of(client));
        ApiClientAuthenticationFilter filter = filter(false);
        MockHttpServletRequest request = post("/api/chat");
        request.addHeader("X-OPIAGILE-API-KEY", "opg_teste");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        verify(repository).markUsed(client.id());
        assertThat(ApiClientContextHolder.current()).isEmpty();
    }

    @Test
    void deveAuditarIpRealQuandoProxyInformarXRealIp() throws Exception {
        ApiClientRecord client = client(Set.of("chat:write"));
        when(repository.findByKeyHash(ApiKeyHasher.sha256Hex("opg_teste"))).thenReturn(Optional.of(client));
        ApiClientAuthenticationFilter filter = filter(false);
        MockHttpServletRequest request = post("/api/chat");
        request.addHeader("X-OPIAGILE-API-KEY", "opg_teste");
        request.addHeader("X-Real-IP", "198.51.100.24");
        request.addHeader("X-Forwarded-For", "203.0.113.250");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        verify(usageLogRepository).save(argThat(log -> "198.51.100.24".equals(log.clientIp())));
    }

    @Test
    void deveBloquearChaveExpirada() throws Exception {
        ApiClientRecord client = client(Set.of("chat:write"), OffsetDateTime.now().minusHours(1));
        when(repository.findByKeyHash(ApiKeyHasher.sha256Hex("opg_teste"))).thenReturn(Optional.of(client));
        ApiClientAuthenticationFilter filter = filter(false);
        MockHttpServletRequest request = post("/api/chat");
        request.addHeader("X-OPIAGILE-API-KEY", "opg_teste");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentAsString()).contains("API_KEY_INVALIDA");
    }

    @Test
    void devePermitirStatusDeProvidersComEscopoCorreto() throws Exception {
        ApiClientRecord client = client(Set.of("providers:read"));
        when(repository.findByKeyHash(ApiKeyHasher.sha256Hex("opg_teste"))).thenReturn(Optional.of(client));
        ApiClientAuthenticationFilter filter = filter(true);
        MockHttpServletRequest request = get("/api/providers/status");
        request.addHeader("X-OPIAGILE-API-KEY", "opg_teste");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        verify(repository).markUsed(client.id());
    }

    private ApiClientAuthenticationFilter filter(boolean requireApiKey) {
        return new ApiClientAuthenticationFilter(
                new ApiSecurityProperties(true, requireApiKey, 60),
                repository,
                usageLogRepository,
                new SimpleRateLimiter(Clock.fixed(Instant.parse("2026-06-27T12:00:00Z"), ZoneOffset.UTC)),
                objectMapper);
    }

    private ApiClientRecord client(Set<String> scopes) {
        return client(scopes, null);
    }

    private ApiClientRecord client(Set<String> scopes, OffsetDateTime expiresAt) {
        TenantContext tenantContext = new TenantContext(
                UUID.randomUUID(),
                "tenant-api",
                UUID.randomUUID(),
                "workspace-api",
                "Workspace API");
        return new ApiClientRecord(
                UUID.randomUUID(),
                "Cliente API",
                "opg_teste",
                ApiKeyHasher.sha256Hex("opg_teste"),
                "ACTIVE",
                scopes,
                60,
                expiresAt,
                tenantContext);
    }

    private MockHttpServletRequest post(String uri) {
        return new MockHttpServletRequest("POST", uri);
    }

    private MockHttpServletRequest get(String uri) {
        return new MockHttpServletRequest("GET", uri);
    }
}
