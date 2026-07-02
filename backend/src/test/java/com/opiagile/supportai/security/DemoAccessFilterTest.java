package com.opiagile.supportai.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import tools.jackson.databind.ObjectMapper;

class DemoAccessFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void devePermitirChatSemTokenQuandoTokenNaoEstaConfigurado() throws Exception {
        DemoAccessFilter filter = filter(policy("", "", true, 10, 10));
        MockHttpServletRequest request = post("/api/chat");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void deveBloquearChatQuandoTokenDaDemoEstaAusente() throws Exception {
        DemoAccessFilter filter = filter(policy("token-demo", "", true, 10, 10));
        MockHttpServletRequest request = post("/api/chat");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getContentAsString()).contains("TOKEN_DEMO_INVALIDO");
    }

    @Test
    void deveAceitarTokenDaDemoPorHeader() throws Exception {
        DemoAccessFilter filter = filter(policy("token-demo", "", true, 10, 10));
        MockHttpServletRequest request = post("/api/documents/upload");
        request.addHeader("X-Demo-Token", "token-demo");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void deveAplicarRateLimitPorIpEEndpoint() throws Exception {
        DemoAccessFilter filter = filter(policy("", "", true, 2, 10));

        assertThat(run(filter, post("/api/chat"))).isEqualTo(HttpStatus.OK.value());
        assertThat(run(filter, post("/api/chat"))).isEqualTo(HttpStatus.OK.value());
        assertThat(run(filter, post("/api/chat"))).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(run(filter, post("/api/documents/upload"))).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void deveUsarXRealIpParaRateLimitQuandoDisponivel() throws Exception {
        DemoAccessFilter filter = filter(policy("", "", true, 1, 10));

        MockHttpServletRequest primeira = post("/api/chat");
        primeira.addHeader("X-Real-IP", "198.51.100.20");
        primeira.addHeader("X-Forwarded-For", "203.0.113.20");

        MockHttpServletRequest segundaMesmoIpReal = post("/api/chat");
        segundaMesmoIpReal.addHeader("X-Real-IP", "198.51.100.20");
        segundaMesmoIpReal.addHeader("X-Forwarded-For", "203.0.113.21");

        MockHttpServletRequest terceiraOutroIpReal = post("/api/chat");
        terceiraOutroIpReal.addHeader("X-Real-IP", "198.51.100.21");
        terceiraOutroIpReal.addHeader("X-Forwarded-For", "203.0.113.20");

        assertThat(run(filter, primeira)).isEqualTo(HttpStatus.OK.value());
        assertThat(run(filter, segundaMesmoIpReal)).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(run(filter, terceiraOutroIpReal)).isEqualTo(HttpStatus.OK.value());
    }

    private int run(DemoAccessFilter filter, MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }

    private DemoAccessFilter filter(DemoSecurityPolicy policy) {
        return new DemoAccessFilter(
                policy,
                new SimpleRateLimiter(Clock.fixed(Instant.parse("2026-06-22T20:00:00Z"), ZoneOffset.UTC)),
                objectMapper);
    }

    private DemoSecurityPolicy policy(String accessToken, String adminToken, boolean rateLimitEnabled, int chatLimit, int uploadLimit) {
        return new DemoSecurityPolicy(accessToken, adminToken, rateLimitEnabled, chatLimit, uploadLimit);
    }

    private MockHttpServletRequest post(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRemoteAddr("203.0.113.10");
        return request;
    }
}
