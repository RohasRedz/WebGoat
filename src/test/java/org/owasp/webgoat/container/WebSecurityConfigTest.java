package org.owasp.webgoat.container;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.users.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Delta tests for WebSecurityConfig focusing on changed behavior:
 * - PasswordEncoder is now BCrypt-based and wired into AuthenticationManagerBuilder.
 * - CSRF is no longer disabled, so Spring Security default CSRF protection is in effect.
 */
public class WebSecurityConfigTest {

    @Test
    @DisplayName("passwordEncoder bean should be a BCryptPasswordEncoder")
    void passwordEncoderShouldBeBCrypt() {
        UserService userService = Mockito.mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);
        PasswordEncoder encoder = config.passwordEncoder();
        assertNotNull(encoder, "PasswordEncoder bean must not be null");
        assertInstanceOf(BCryptPasswordEncoder.class, encoder,
                "PasswordEncoder must be an instance of BCryptPasswordEncoder");
    }

    @Test
    @DisplayName("configureGlobal must register passwordEncoder with AuthenticationManagerBuilder")
    void configureGlobalShouldUsePasswordEncoder() throws Exception {
        UserService userService = Mockito.mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);
        AuthenticationManagerBuilder authBuilder = Mockito.mock(AuthenticationManagerBuilder.class);
        Mockito.when(authBuilder.userDetailsService(Mockito.any(UserDetailsService.class))).thenReturn(authBuilder);
        Mockito.when(authBuilder.passwordEncoder(Mockito.any(PasswordEncoder.class))).thenReturn(authBuilder);
        config.configureGlobal(authBuilder);
        Mockito.verify(authBuilder).userDetailsService(userService);
        Mockito.verify(authBuilder).passwordEncoder(Mockito.any(PasswordEncoder.class));
    }

    @Test
    @DisplayName("filterChain should not explicitly disable CSRF protection")
    void filterChainShouldNotDisableCsrf() throws Exception {
        UserService userService = Mockito.mock(UserService.class);
        WebSecurityConfig config = new WebSecurityConfig(userService);
        HttpSecurity http = Mockito.mock(HttpSecurity.class, Mockito.RETURNS_DEEP_STUBS);
        config.filterChain(http);
        Mockito.verify(http, Mockito.never()).csrf(Mockito.any());
    }
}
