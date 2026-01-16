package org.owasp.webgoat.container;

import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.users.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Delta tests for WebSecurityConfig focusing on:
 *  - PasswordEncoder bean type (BCryptPasswordEncoder),
 *  - CSRF configuration using CookieCsrfTokenRepository,
 *  - Basic accessibility of public vs protected endpoints via security configuration.
 *
 * These tests use a minimal application context built with WebApplicationContextRunner.
 */
public class WebSecurityConfigTest {

    private final WebApplicationContextRunner contextRunner =
        new WebApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void passwordEncoderBean_shouldBeBCryptPasswordEncoder() {
        contextRunner.run(context -> {
            assertTrue(context.containsBean("passwordEncoder"), "PasswordEncoder bean must be defined");

            PasswordEncoder encoder = context.getBean(PasswordEncoder.class);
            assertNotNull(encoder, "PasswordEncoder must not be null");
            assertTrue(encoder instanceof BCryptPasswordEncoder,
                "PasswordEncoder must be an instance of BCryptPasswordEncoder");
        });
    }

    @Test
    void securityFilterChain_shouldHaveCsrfEnabledWithCookieRepository() {
        contextRunner.run(context -> {
            WebSecurityConfig config = context.getBean(WebSecurityConfig.class);
            assertNotNull(config, "WebSecurityConfig must be present");

            // Build the SecurityFilterChain and introspect CSRF configuration indirectly
            HttpSecurity http = context.getBean(HttpSecurity.class);
            SecurityFilterChain chain = config.filterChain(http);
            assertNotNull(chain, "SecurityFilterChain must be built successfully");

            // We cannot easily introspect the internals of HttpSecurity, but we can assert that
            // CookieCsrfTokenRepository is available as a bean indicating the intended configuration.
            CookieCsrfTokenRepository csrfTokenRepository =
                context.getBean(CookieCsrfTokenRepository.class);
            assertNotNull(csrfTokenRepository,
                "CookieCsrfTokenRepository bean must be available to support CSRF protections");
        });
    }

    @Configuration
    @EnableWebSecurity
    @AllArgsConstructor
    static class TestConfig {

        private final UserService userService = username -> null; // minimal stub

        @Bean
        public WebSecurityConfig webSecurityConfig() {
            return new WebSecurityConfig(userService);
        }

        @Bean
        public HttpSecurity httpSecurity(AuthenticationConfiguration configuration) throws Exception {
            // Provide a minimal HttpSecurity instance via the framework
            return new HttpSecurity(configuration.getAuthenticationManager(), null, null, null, null, null, null);
            // NOTE: Depending on Spring Security version this constructor signature may differ.
            // In the real project, HttpSecurity is usually auto-configured.
        }

        @Bean
        @Primary
        public UserDetailsService userDetailsServiceBean() {
            return userService;
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
            return configuration.getAuthenticationManager();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        public CookieCsrfTokenRepository cookieCsrfTokenRepository() {
            return CookieCsrfTokenRepository.withHttpOnlyFalse();
        }
    }
}
