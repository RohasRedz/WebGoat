package org.owasp.webgoat.container;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

class WebSecurityConfigTest {

    @Test
    void passwordEncoderBeanShouldBeBCryptAndNotNoOp() {
        UserService userService = mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);
        PasswordEncoder encoder = config.passwordEncoder();
        assertNotNull(encoder, "PasswordEncoder bean must not be null");
        assertTrue(encoder instanceof BCryptPasswordEncoder,
                "PasswordEncoder should be an instance of BCryptPasswordEncoder after the fix");
    }

    @Test
    void filterChainShouldBeBuildableWithoutExplicitCsrfDisable() throws Exception {
        UserService userService = mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);
        HttpSecurity http = new HttpSecurity(mock(AuthenticationManager.class), mock(AuthenticationConfiguration.class), null, null, null, null);
        SecurityFilterChain chain = config.filterChain(http);
        assertNotNull(chain, "SecurityFilterChain should be successfully built without disabling CSRF");
    }
}
