package com.hieu.analytics_service.config;

import com.hieu.common.security.AuthenticatedUser;
import com.hieu.common.security.JwtTokenValidator;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

/**
 * Stateless JWT authentication filter.
 * Supports Bearer token hoặc ACCESS_TOKEN cookie.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String ACCESS_COOKIE = "ACCESS_TOKEN";
    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtTokenValidator validator;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var token = extractToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                if (validator.isExpired(token)) {
                    log.debug("Rejected expired token");
                } else {
                    Claims claims = validator.parseClaims(token);
                    var user = new AuthenticatedUser(
                            claims.get("userId", String.class),
                            claims.getSubject(),
                            extractList(claims, "roles"),
                            extractList(claims, "permissions"));
                    var authorities = Stream.concat(user.roles().stream(), user.permissions().stream())
                            .map(SimpleGrantedAuthority::new)
                            .toList();
                    var auth = new UsernamePasswordAuthenticationToken(user, token, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                log.debug("JWT parse failed: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    @SuppressWarnings("unchecked")
    private static List<String> extractList(Claims claims, String key) {
        Object raw = claims.get(key);
        return raw instanceof List<?> list
                ? list.stream().map(Object::toString).toList()
                : List.of();
    }

    private static String extractToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if (ACCESS_COOKIE.equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                    return c.getValue();
                }
            }
        }
        var h = request.getHeader(HEADER);
        if (h == null || !h.startsWith(PREFIX)) return null;
        var t = h.substring(PREFIX.length()).trim();
        return t.isEmpty() ? null : t;
    }
}
