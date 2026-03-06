package com.offerverdict.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CanonicalHostRedirectFilterTest {

    @Test
    void redirectsWhenHostDiffersFromCanonical() throws ServletException, IOException {
        AppProperties props = new AppProperties();
        props.setPublicBaseUrl("https://livingcostcheck.com");
        props.setEnforceCanonicalHostRedirect(true);
        CanonicalHostRedirectFilter filter = new CanonicalHostRedirectFilter(props);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/salary-check/austin-tx/120000");
        request.setServerName("www.livingcostcheck.com");
        request.setServerPort(443);
        request.setScheme("https");
        request.setQueryString("ref=abc");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);

        assertEquals(301, response.getStatus());
        assertEquals("https://livingcostcheck.com/salary-check/austin-tx/120000?ref=abc",
                response.getHeader("Location"));
    }

    @Test
    void passesThroughWhenHostAlreadyCanonical() throws ServletException, IOException {
        AppProperties props = new AppProperties();
        props.setPublicBaseUrl("https://livingcostcheck.com");
        props.setEnforceCanonicalHostRedirect(true);
        CanonicalHostRedirectFilter filter = new CanonicalHostRedirectFilter(props);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setServerName("livingcostcheck.com");
        request.setServerPort(443);
        request.setScheme("https");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNull(response.getHeader("Location"));
    }

    @Test
    void passesThroughWhenProxyHeadersMatchCanonicalWithoutForwardedPort() throws ServletException, IOException {
        AppProperties props = new AppProperties();
        props.setPublicBaseUrl("https://livingcostcheck.com");
        props.setEnforceCanonicalHostRedirect(true);
        CanonicalHostRedirectFilter filter = new CanonicalHostRedirectFilter(props);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setServerName("127.0.0.1");
        request.setServerPort(8080);
        request.setScheme("http");
        request.addHeader("X-Forwarded-Host", "livingcostcheck.com");
        request.addHeader("X-Forwarded-Proto", "https");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNull(response.getHeader("Location"));
    }

    @Test
    void skipsLocalhostToAvoidDevRedirects() throws ServletException, IOException {
        AppProperties props = new AppProperties();
        props.setPublicBaseUrl("https://livingcostcheck.com");
        props.setEnforceCanonicalHostRedirect(true);
        CanonicalHostRedirectFilter filter = new CanonicalHostRedirectFilter(props);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setServerName("localhost");
        request.setServerPort(8080);
        request.setScheme("http");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNull(response.getHeader("Location"));
    }
}
