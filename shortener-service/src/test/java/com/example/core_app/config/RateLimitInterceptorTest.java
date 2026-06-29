package com.example.core_app.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitInterceptorTest {

    private RateLimitInterceptor interceptor;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        RateLimitProperties props = new RateLimitProperties();
        props.setShortenLimit(3);
        props.setRedirectLimit(5);
        props.setWindowSeconds(60);
        interceptor = new RateLimitInterceptor(props);
        response = new MockHttpServletResponse();
    }

    @Test
    void allowsRequestsUnderShortenLimit() throws Exception {
        MockHttpServletRequest request = buildRequest("POST", "/api/v1/shorten", "10.0.0.1");

        for (int i = 0; i < 3; i++) {
            assertThat(interceptor.preHandle(request, response, null)).isTrue();
        }
    }

    @Test
    void returns429_whenShortenLimitExceeded() throws Exception {
        MockHttpServletRequest request = buildRequest("POST", "/api/v1/shorten", "10.0.0.2");

        for (int i = 0; i < 3; i++) {
            interceptor.preHandle(request, response, null);
        }

        boolean allowed = interceptor.preHandle(request, response, null);

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
    }

    @Test
    void allowsRequestsUnderRedirectLimit() throws Exception {
        MockHttpServletRequest request = buildRequest("GET", "/abc123", "10.0.0.3");

        for (int i = 0; i < 5; i++) {
            assertThat(interceptor.preHandle(request, response, null)).isTrue();
        }
    }

    @Test
    void returns429_whenRedirectLimitExceeded() throws Exception {
        MockHttpServletRequest request = buildRequest("GET", "/abc123", "10.0.0.4");

        for (int i = 0; i < 5; i++) {
            interceptor.preHandle(request, response, null);
        }

        boolean allowed = interceptor.preHandle(request, response, null);

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void bucketsAreIsolatedPerIp() throws Exception {
        MockHttpServletRequest ipA = buildRequest("POST", "/api/v1/shorten", "10.0.0.5");
        MockHttpServletRequest ipB = buildRequest("POST", "/api/v1/shorten", "10.0.0.6");

        for (int i = 0; i < 3; i++) {
            interceptor.preHandle(ipA, response, null);
        }

        // ipA exhausted its bucket, ipB should still pass
        assertThat(interceptor.preHandle(ipA, response, null)).isFalse();
        assertThat(interceptor.preHandle(ipB, new MockHttpServletResponse(), null)).isTrue();
    }

    @Test
    void resolvesClientIp_fromXForwardedForHeader() throws Exception {
        MockHttpServletRequest request = buildRequest("POST", "/api/v1/shorten", "proxy-ip");
        request.addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.7");

        // Exhaust the bucket for the real client IP
        for (int i = 0; i < 3; i++) {
            interceptor.preHandle(request, response, null);
        }
        assertThat(interceptor.preHandle(request, response, null)).isFalse();

        // Same request from proxy but different client IP should still pass
        MockHttpServletRequest other = buildRequest("POST", "/api/v1/shorten", "proxy-ip");
        other.addHeader("X-Forwarded-For", "203.0.113.99");
        assertThat(interceptor.preHandle(other, new MockHttpServletResponse(), null)).isTrue();
    }

    private MockHttpServletRequest buildRequest(String method, String uri, String remoteAddr) {
        MockHttpServletRequest req = new MockHttpServletRequest(method, uri);
        req.setRemoteAddr(remoteAddr);
        return req;
    }
}
