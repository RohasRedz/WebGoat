/* Delta tests for WebSecurityConfig focusing on security configuration changes. */
package org.owasp.webgoat.container;

import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WebSecurityConfigTest {

    @Test
    void passwordEncoder_shouldReturnBCryptBasedPasswordEncoder() {
        WebSecurityConfig config = new WebSecurityConfig(null);

        PasswordEncoder encoder = config.passwordEncoder();

        assertThat(encoder).isNotNull();
        assertThat(encoder.getClass().getSimpleName()).isEqualTo("BCryptPasswordEncoder");
        String raw = "secret123";
        String encoded = encoder.encode(raw);
        assertThat(encoded).isNotEqualTo(raw);
        assertThat(encoder.matches(raw, encoded)).isTrue();
    }

    @Test
    void filterChain_shouldConfigureCsrf() throws Exception {
        WebSecurityConfig config = new WebSecurityConfig(null);
        HttpSecurity http = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);

        SecurityFilterChain chain = config.filterChain(http);

        assertThat(chain).isNotNull();
        verify(http).csrf(any());
        verify(http).headers(any());
    }
}
