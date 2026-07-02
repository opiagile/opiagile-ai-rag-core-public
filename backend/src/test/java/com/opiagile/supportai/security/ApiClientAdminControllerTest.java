package com.opiagile.supportai.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ApiClientAdminControllerTest {

    private final ApiClientUsageLogRepository repository = mock(ApiClientUsageLogRepository.class);
    private final DemoSecurityPolicy securityPolicy = new DemoSecurityPolicy("", "admin-token", true, 10, 10);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ApiClientAdminController(repository, securityPolicy)).build();

    @Test
    void deveBloquearConsultaSemTokenAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/api-clients/usage"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRetornarRelatorioComTokenAdmin() throws Exception {
        when(repository.summarizeByClient(50)).thenReturn(List.of());
        when(repository.findRecent(50)).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/api-clients/usage")
                        .header("X-Demo-Admin-Token", "admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summaries").isArray())
                .andExpect(jsonPath("$.recentEvents").isArray());
    }
}
