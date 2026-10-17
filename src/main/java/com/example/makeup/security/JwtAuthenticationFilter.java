package com.example.makeup.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

import com.example.makeup.auth.CustomUserDetailsService;
import com.example.makeup.auth.JwtService;
import com.example.makeup.auth.OidcProperties;
import com.example.makeup.auth.OidcUserProvisioner;
import com.example.makeup.auth.User;

/**
 * Аутентификация по Bearer-токену. Поддерживает два источника:
 * <ol>
 *   <li>access-токен единого IdP (auth-service, RS256) — проверяется по JWKS,
 *       пользователь находится/создаётся по claim {@code sub};</li>
 *   <li>legacy HS256-токен makeup (собственный логин/пароль) — на время миграции.</li>
 * </ol>
 * Principal в обоих случаях — пользователь, чей {@code getUsername()} равен
 * локальному {@code users.username} (как и раньше).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final OidcUserProvisioner oidcUserProvisioner;
    private final OidcProperties oidcProperties;
    private final ObjectProvider<JwtDecoder> oidcDecoderProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        try {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                if (!authenticateWithOidc(jwt, request)) {
                    authenticateWithLegacyToken(jwt, request);
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /** Access-токен auth-service (RS256). Возвращает true, если он принят. */
    private boolean authenticateWithOidc(String token, HttpServletRequest request) {
        if (!oidcProperties.enabled()) {
            return false;
        }
        JwtDecoder decoder = oidcDecoderProvider.getIfAvailable();
        if (decoder == null) {
            return false;
        }
        try {
            Jwt jwt = decoder.decode(token);
            UUID subject = UUID.fromString(jwt.getSubject());
            User user = oidcUserProvisioner.findOrCreate(subject,
                    jwt.getClaimAsString("username"), jwt.getClaimAsString("email"));
            if (user.isEnabled()) {
                setAuthentication(user, request);
            }
            log.debug("Authenticated auth subject {} as makeup user {}", subject, user.getUsername());
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token is not a valid auth-service access token: {}", e.getMessage());
            return false;
        }
    }

    /** Legacy HS256-токен, выданный самим makeup. */
    private void authenticateWithLegacyToken(String token, HttpServletRequest request) {
        Claims claims;
        try {
            claims = jwtService.parseClaims(token);
        } catch (Exception e) {
            return;
        }
        String username = claims.getSubject();

        if (username != null && !jwtService.isTokenExpired(claims)) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
            if (userDetails.getUsername().equals(username) && userDetails.isEnabled()) {
                setAuthentication(userDetails, request);
            }
        }
    }

    private void setAuthentication(UserDetails userDetails, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}
