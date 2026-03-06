package com.offerverdict.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class CanonicalHostRedirectFilter extends OncePerRequestFilter {

    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "::1", "[::1]");
    private final AppProperties appProperties;

    public CanonicalHostRedirectFilter(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!appProperties.isEnforceCanonicalHostRedirect()) {
            return true;
        }
        String method = request.getMethod();
        return !("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        URI canonicalBase = resolveCanonicalBaseUri();
        if (canonicalBase == null || canonicalBase.getHost() == null || canonicalBase.getScheme() == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestScheme = resolveRequestScheme(request);
        String requestHost = resolveRequestHost(request);
        int requestPort = resolveRequestPort(request, requestScheme);

        if (requestHost == null || isLocalHost(requestHost)) {
            filterChain.doFilter(request, response);
            return;
        }

        String canonicalScheme = canonicalBase.getScheme();
        String canonicalHost = canonicalBase.getHost();
        int canonicalPort = normalizePort(canonicalBase.getPort(), canonicalScheme);
        int normalizedRequestPort = normalizePort(requestPort, requestScheme);

        boolean sameScheme = canonicalScheme.equalsIgnoreCase(requestScheme);
        boolean sameHost = canonicalHost.equalsIgnoreCase(requestHost);
        boolean samePort = canonicalPort == normalizedRequestPort;

        // Some reverse proxies preserve Host but do not forward the external scheme/port.
        // In that case redirecting to the same canonical URL causes an infinite loop.
        if (sameHost && (!sameScheme || !samePort) && !hasExplicitSchemeSignal(request) && !hasExplicitPortSignal(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (sameScheme && sameHost && samePort) {
            filterChain.doFilter(request, response);
            return;
        }

        String redirectUrl = UriComponentsBuilder
                .fromUri(canonicalBase)
                .replacePath(request.getRequestURI())
                .replaceQuery(request.getQueryString())
                .build(true)
                .toUriString();

        response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        response.setHeader("Location", redirectUrl);
    }

    private URI resolveCanonicalBaseUri() {
        String baseUrl = appProperties.getPublicBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }
        try {
            return URI.create(baseUrl.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String resolveRequestScheme(HttpServletRequest request) {
        String forwardedProtoFromHeader = forwardedPairValue(request, "proto");
        if (forwardedProtoFromHeader != null && !forwardedProtoFromHeader.isBlank()) {
            return forwardedProtoFromHeader.toLowerCase(Locale.US);
        }

        String forwardedProto = firstHeaderToken(request.getHeader("X-Forwarded-Proto"));
        if (forwardedProto != null && !forwardedProto.isBlank()) {
            return forwardedProto.toLowerCase(Locale.US);
        }

        if ("on".equalsIgnoreCase(firstHeaderToken(request.getHeader("X-Forwarded-Ssl")))
                || "on".equalsIgnoreCase(firstHeaderToken(request.getHeader("Front-End-Https")))) {
            return "https";
        }

        String cfVisitorScheme = cfVisitorScheme(request.getHeader("CF-Visitor"));
        if (cfVisitorScheme != null) {
            return cfVisitorScheme;
        }

        if (request.isSecure()) {
            return "https";
        }

        return request.getScheme() != null ? request.getScheme().toLowerCase(Locale.US) : "https";
    }

    private String resolveRequestHost(HttpServletRequest request) {
        String forwardedHost = forwardedPairValue(request, "host");
        if (forwardedHost == null) {
            forwardedHost = firstHeaderToken(request.getHeader("X-Forwarded-Host"));
        }
        String hostHeader = firstHeaderToken(request.getHeader("Host"));
        String candidate = forwardedHost != null
                ? forwardedHost
                : (hostHeader != null ? hostHeader : request.getServerName());
        if (candidate == null || candidate.isBlank()) {
            return null;
        }

        candidate = candidate.trim();
        if (candidate.startsWith("[") && candidate.contains("]")) {
            int end = candidate.indexOf(']');
            return candidate.substring(0, end + 1);
        }

        int colonIndex = candidate.lastIndexOf(':');
        if (colonIndex > 0 && candidate.indexOf(':') == colonIndex) {
            return candidate.substring(0, colonIndex);
        }
        return candidate;
    }

    private int resolveRequestPort(HttpServletRequest request, String requestScheme) {
        String forwardedPort = firstHeaderToken(request.getHeader("X-Forwarded-Port"));
        if (forwardedPort != null) {
            try {
                return Integer.parseInt(forwardedPort);
            } catch (NumberFormatException ignored) {
                // Fallback below.
            }
        }

        String forwardedHost = forwardedPairValue(request, "host");
        if (forwardedHost == null) {
            forwardedHost = firstHeaderToken(request.getHeader("X-Forwarded-Host"));
        }
        Integer explicitForwardedPort = extractExplicitPort(forwardedHost);
        if (explicitForwardedPort != null) {
            return explicitForwardedPort;
        }
        if (forwardedHost != null && !forwardedHost.isBlank()) {
            return normalizePort(-1, requestScheme);
        }

        String hostHeader = firstHeaderToken(request.getHeader("Host"));
        Integer explicitHostPort = extractExplicitPort(hostHeader);
        if (explicitHostPort != null) {
            return explicitHostPort;
        }
        if (hostHeader != null && !hostHeader.isBlank()) {
            return normalizePort(-1, requestScheme);
        }

        return normalizePort(request.getServerPort(), requestScheme);
    }

    private boolean hasExplicitSchemeSignal(HttpServletRequest request) {
        return forwardedPairValue(request, "proto") != null
                || hasText(firstHeaderToken(request.getHeader("X-Forwarded-Proto")))
                || hasText(firstHeaderToken(request.getHeader("X-Forwarded-Ssl")))
                || hasText(firstHeaderToken(request.getHeader("Front-End-Https")))
                || cfVisitorScheme(request.getHeader("CF-Visitor")) != null
                || request.isSecure();
    }

    private boolean hasExplicitPortSignal(HttpServletRequest request) {
        return hasText(firstHeaderToken(request.getHeader("X-Forwarded-Port")))
                || extractExplicitPort(forwardedPairValue(request, "host")) != null
                || extractExplicitPort(firstHeaderToken(request.getHeader("X-Forwarded-Host"))) != null
                || extractExplicitPort(firstHeaderToken(request.getHeader("Host"))) != null;
    }

    private Integer extractExplicitPort(String hostHeader) {
        if (hostHeader == null || hostHeader.isBlank()) {
            return null;
        }

        String host = hostHeader.trim();
        if (host.startsWith("[") && host.contains("]")) {
            int end = host.indexOf(']');
            if (host.length() > end + 2 && host.charAt(end + 1) == ':') {
                try {
                    return Integer.parseInt(host.substring(end + 2));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
            return null;
        }

        int colonIndex = host.lastIndexOf(':');
        if (colonIndex > 0 && host.indexOf(':') == colonIndex) {
            try {
                return Integer.parseInt(host.substring(colonIndex + 1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String forwardedPairValue(HttpServletRequest request, String key) {
        String forwarded = request.getHeader("Forwarded");
        if (forwarded == null || forwarded.isBlank()) {
            return null;
        }

        String firstEntry = firstHeaderToken(forwarded);
        String[] parts = firstEntry.split(";");
        for (String part : parts) {
            int separator = part.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String candidateKey = part.substring(0, separator).trim();
            if (!candidateKey.equalsIgnoreCase(key)) {
                continue;
            }
            String value = part.substring(separator + 1).trim();
            if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            return value;
        }
        return null;
    }

    private String cfVisitorScheme(String cfVisitorHeader) {
        if (cfVisitorHeader == null || cfVisitorHeader.isBlank()) {
            return null;
        }

        String normalized = cfVisitorHeader.trim().toLowerCase(Locale.US);
        if (normalized.contains("\"scheme\":\"https\"")) {
            return "https";
        }
        if (normalized.contains("\"scheme\":\"http\"")) {
            return "http";
        }
        return null;
    }

    private int normalizePort(int port, String scheme) {
        if (port > 0) {
            return port;
        }
        return "http".equalsIgnoreCase(scheme) ? 80 : 443;
    }

    private String firstHeaderToken(String rawHeader) {
        if (rawHeader == null || rawHeader.isBlank()) {
            return null;
        }
        int commaIndex = rawHeader.indexOf(',');
        return commaIndex >= 0 ? rawHeader.substring(0, commaIndex).trim() : rawHeader.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean isLocalHost(String host) {
        String normalized = host.trim().toLowerCase(Locale.US);
        return LOCAL_HOSTS.contains(normalized);
    }
}
