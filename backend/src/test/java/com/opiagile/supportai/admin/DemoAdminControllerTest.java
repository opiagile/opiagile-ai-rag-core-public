package com.opiagile.supportai.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.opiagile.supportai.security.DemoSecurityPolicy;

class DemoAdminControllerTest {

    private final DemoAdminService service = mock(DemoAdminService.class);

    @Test
    void deveBloquearResetQuandoTokenAdminNaoFoiConfigurado() {
        DemoAdminController controller = new DemoAdminController(service, policy("", ""));

        ResponseEntity<?> response = controller.reset(new MockHttpServletRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(service, never()).resetDemoData();
    }

    @Test
    void deveBloquearResetComTokenInvalido() {
        DemoAdminController controller = new DemoAdminController(service, policy("", "admin-token"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Demo-Admin-Token", "errado");

        ResponseEntity<?> response = controller.reset(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(service, never()).resetDemoData();
    }

    @Test
    void deveExecutarResetComTokenValido() {
        DemoResetResponse reset = new DemoResetResponse(1, 2, 3, 4, 5, 6, 7, 8);
        when(service.resetDemoData()).thenReturn(reset);
        DemoAdminController controller = new DemoAdminController(service, policy("", "admin-token"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Demo-Admin-Token", "admin-token");

        ResponseEntity<?> response = controller.reset(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(reset);
        verify(service).resetDemoData();
    }

    private DemoSecurityPolicy policy(String accessToken, String adminToken) {
        return new DemoSecurityPolicy(accessToken, adminToken, true, 30, 5);
    }
}
