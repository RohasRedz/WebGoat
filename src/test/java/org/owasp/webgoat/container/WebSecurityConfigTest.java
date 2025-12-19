package org.owasp.webgoat.container;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.users.UserService;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;

public class WebSecurityConfigTest {

    private WebSecurityConfig createConfig() {
        UserService userService = Mockito.mock(UserService.class);
        return new WebSecurityConfig(userService);
    }

    @Test
    void passwordEncoder_shouldNotBeNoOpAndShouldHashPassword() {
        WebSecurityConfig config = createConfig();
        PasswordEncoder encoder = config.passwordEncoder();
        String rawPassword = "secret123";
        String encoded = encoder.encode(rawPassword);

        assertThat(encoder).isNotNull();
        assertThat(encoded).isNotEqualTo(rawPassword);
        assertThat(encoder.matches(rawPassword, encoded)).isTrue();
    }
}
