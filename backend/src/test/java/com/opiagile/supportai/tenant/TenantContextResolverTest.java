package com.opiagile.supportai.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.opiagile.supportai.security.ApiClientContext;
import com.opiagile.supportai.security.ApiClientContextHolder;

class TenantContextResolverTest {

    private final TenantRepository tenantRepository = mock(TenantRepository.class);
    private final TenantContextResolver resolver = new TenantContextResolver(tenantRepository, "demo", "clinica-demo");

    @AfterEach
    void limparContexto() {
        ApiClientContextHolder.clear();
    }

    @Test
    void devePriorizarTenantDaChaveApiAutenticada() {
        TenantContext tenantContext = new TenantContext(
                UUID.randomUUID(),
                "tenant-api",
                UUID.randomUUID(),
                "workspace-api",
                "Workspace API");
        ApiClientContextHolder.set(new ApiClientContext(UUID.randomUUID(), "Cliente API", tenantContext, Set.of("chat:write"), 60));

        TenantContext resolved = resolver.resolve("tenant-header", "workspace-header");

        assertThat(resolved).isEqualTo(tenantContext);
        verify(tenantRepository, never()).findContext("tenant-header", "workspace-header");
    }

    @Test
    void deveResolverTenantDoHeaderQuandoNaoHaChaveApi() {
        TenantContext tenantContext = new TenantContext(
                UUID.randomUUID(),
                "tenant-header",
                UUID.randomUUID(),
                "workspace-header",
                "Workspace Header");
        when(tenantRepository.findContext("tenant-header", "workspace-header")).thenReturn(java.util.Optional.of(tenantContext));

        TenantContext resolved = resolver.resolve("Tenant-Header", "Workspace-Header");

        assertThat(resolved).isEqualTo(tenantContext);
    }
}
