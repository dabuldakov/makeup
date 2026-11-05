package com.example.makeup.security;

import com.example.makeup.auth.OidcProperties;
import com.example.makeup.auth.OidcUserProvisioner;
import com.example.makeup.auth.Role;
import com.example.makeup.auth.User;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private OidcUserProvisioner provisioner;
    private ObjectProvider<JwtDecoder> decoderProvider;
    private JwtDecoder decoder;
    private FilterChain chain;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        provisioner = mock(OidcUserProvisioner.class);
        decoderProvider = mock(ObjectProvider.class);
        decoder = mock(JwtDecoder.class);
        chain = mock(FilterChain.class);
        filter = new JwtAuthenticationFilter(
                provisioner,
                new OidcProperties(true, "issuer", "jwks", List.of("api-muzea")),
                decoderProvider
        );
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest request(String token) {
        var request = new MockHttpServletRequest("GET", "/api/users/me");
        request.setServletPath("/api/users/me");
        if (token != null) {
            request.addHeader("Authorization", "Bearer " + token);
        }
        return request;
    }

    @Test
    void validTokenAuthenticatesEnabledUser() throws Exception {
        UUID subject = UUID.randomUUID();
        User user = User.builder()
                .id(1L)
                .username("alice")
                .fullName("Alice")
                .role(Role.USER)
                .isActive(true)
                .build();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(subject.toString())
                .claim("username", "alice")
                .build();
        when(decoderProvider.getIfAvailable()).thenReturn(decoder);
        when(decoder.decode("token")).thenReturn(jwt);
        when(provisioner.findOrCreate(subject, "alice")).thenReturn(user);

        filter.doFilter(request("token"), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isSameAs(user);
        verify(chain).doFilter(any(), any());
    }

    @Test
    void disabledUserIsNotAuthenticated() throws Exception {
        UUID subject = UUID.randomUUID();
        User user = User.builder()
                .id(2L)
                .username("blocked")
                .fullName("Blocked")
                .role(Role.USER)
                .isActive(false)
                .build();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(subject.toString())
                .claim("username", "blocked")
                .build();
        when(decoderProvider.getIfAvailable()).thenReturn(decoder);
        when(decoder.decode("token")).thenReturn(jwt);
        when(provisioner.findOrCreate(subject, "blocked")).thenReturn(user);

        filter.doFilter(request("token"), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void invalidTokenLeavesAnonymous() throws Exception {
        when(decoderProvider.getIfAvailable()).thenReturn(decoder);
        when(decoder.decode("bad")).thenThrow(new JwtException("bad"));

        filter.doFilter(request("bad"), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(any(), any());
    }

    @Test
    void disabledOidcDoesNotAuthenticate() throws Exception {
        filter = new JwtAuthenticationFilter(
                provisioner,
                new OidcProperties(false, "issuer", "jwks", List.of("api-muzea")),
                decoderProvider
        );

        filter.doFilter(request("token"), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(decoderProvider);
    }

    @Test
    void missingDecoderDoesNotAuthenticate() throws Exception {
        when(decoderProvider.getIfAvailable()).thenReturn(null);

        filter.doFilter(request("token"), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void missingBearerHeaderIsSkipped() throws Exception {
        filter.doFilter(request(null), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(any(), any());
    }
}
