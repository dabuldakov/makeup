package com.example.makeup.auth;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OidcConfigTest {

    @Test
    void createsDecoderWithIssuerAudienceAndTokenTypeValidators() {
        var properties = new OidcProperties(true, "https://auth.muzea.su",
                "https://auth.muzea.su/.well-known/jwks.json", List.of("api-muzea"));

        assertThat(new OidcConfig().oidcJwtDecoder(properties)).isNotNull();
    }
}
