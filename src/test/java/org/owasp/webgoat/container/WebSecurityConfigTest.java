// Derived package from the source file under test.
package org.owasp.webgoat.container;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.owasp.webgoat.container.users.UserService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.LazyCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Delta unit tests for {@link WebSecurityConfig}.
 *
 * These tests focus ONLY on the security changes introduced:
 * - CSRF protection must not be disabled anymore.
 * - PasswordEncoder bean must be a secure encoder (BCryptPasswordEncoder).
 */
public class WebSecurityConfigTest {

    /**
     * Minimal stub implementation of UserService to satisfy WebSecurityConfig constructor.
     * The behavior of UserService is outside the delta scope, so we keep it minimal.
     */
    private static class StubUserService implements UserService {
        // TODO: Implement only methods actually required by Spring, if compilation fails.
    }

    /**
     * Verifies that the PasswordEncoder bean provided by WebSecurityConfig is a BCrypt-based encoder
     * (i.e., NoOpPasswordEncoder is no longer used).
     */
    @Test
    @DisplayName("passwordEncoder() should return a BCrypt-based PasswordEncoder")
    void passwordEncoderShouldBeBCrypt() {
        // Arrange
        WebSecurityConfig config = new WebSecurityConfig(new StubUserService());

        // Act
        PasswordEncoder encoder = config.passwordEncoder();

        // Assert
        assertThat(encoder).isNotNull();
        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);

        // Additional behavioral assertion to ensure it is not a no-op encoder:
        String raw = "secret";
        String encoded = encoder.encode(raw);
        assertThat(encoded).isNotEqualTo(raw);
        assertThat(encoder.matches(raw, encoded)).isTrue();
    }

    /**
     * Verifies, in a delta-focused way, that CSRF protection is enabled in the SecurityFilterChain:
     * - Before the fix, CSRF was explicitly disabled via csrf.disable().
     * - After the fix, the chain should include a CsrfFilter (or equivalent) and not explicitly disable CSRF.
     *
     * NOTE: This test uses reflection and type inspection to avoid needing a full Spring context.
     */
    @Test
    @DisplayName("SecurityFilterChain should have CSRF protection enabled (no explicit disable)")
    void securityFilterChainShouldEnableCsrfByDefault() throws Exception {
        // Arrange: create config with stub dependencies
        WebSecurityConfig config = new WebSecurityConfig(new StubUserService());

        // We cannot easily construct a full HttpSecurity instance without Spring context,
        // so instead we validate the behavior indirectly via SecurityFilterChain.
        // To do this in a context-less way, we reflectively locate the filters that would
        // typically be present when CSRF is enabled.

        // We create a mock AuthenticationConfiguration just to call authenticationManager;
        // its behavior is irrelevant for this delta test.
        AuthenticationConfiguration authenticationConfiguration =
                ReflectionTestUtils.invokeConstructor(AuthenticationConfiguration.class);

        // Act
        SecurityFilterChain chain = config.filterChain(
                ReflectionTestUtils.invokeConstructor(
                        org.springframework.security.config.annotation.web.builders.HttpSecurity.class,
                        null, null, null, null, null, null));

        List<Filter> filters = chain.getFilters();

        // Assert: ensure at least one CsrfFilter (or subclass) is present in the chain.
        boolean hasCsrfFilter = filters.stream().anyMatch(f -> f instanceof CsrfFilter);
        assertFalse(filters.stream().anyMatch(f -> f.getClass().getSimpleName().contains("DisableCsrf")),
                "Filter chain should not contain any filter that explicitly disables CSRF");
        assertThat(hasCsrfFilter)
                .as("SecurityFilterChain should contain a CsrfFilter when CSRF is enabled by default")
                .isTrue();
    }

    /**
     * Additional, more isolated assertion that Spring's default CSRF semantics apply:
     * if we obtain a CsrfTokenRepository and request a token, a non-null token should be generated.
     *
     * This does not wire through HttpSecurity directly but asserts the expectations
     * that accompany the fix (i.e., CSRF tokens are used instead of being disabled).
     */
    @Test
    @DisplayName("CSRF token repository should generate non-null CSRF tokens")
    void csrfTokenRepositoryShouldGenerateTokens() {
        // Arrange
        CsrfTokenRepository repo =
                new LazyCsrfTokenRepository(new HttpSessionCsrfTokenRepository());

        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new org.springframework.mock.web.MockHttpServletResponse();

        // Act
        CsrfToken token = repo.generateToken(request);
        repo.saveToken(token, request, response);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token.getToken()).isNotBlank();
    }
}
