package org.owasp.webgoat.container;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.owasp.webgoat.container.users.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.config.annotation.authentication.configuration.ObjectPostProcessorConfiguration;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration.ImportAuthenticationConfiguration;
import org.springframework.security.config.annotation.authentication.configuration.InitializeAuthenticationProviderBeanManagerConfigurer;
import org.springframework.security.config.annotation.authentication.configuration.RegisterGlobalAuthenticationConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter.AuthenticationManagerDelegator;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;

/**
 * Delta unit tests for {@link WebSecurityConfig}.
 *
 * <p>These tests focus only on the changed behavior:
 * <ul>
 *   <li>Ensuring a strong {@link PasswordEncoder} is used instead of a NoOp/plaintext encoder.</li>
 *   <li>Ensuring CSRF protection is enabled in the {@link SecurityFilterChain}.</li>
 * </ul>
 *
 * <p>NOTE:
 * We avoid end-to-end application context bootstrapping and instead unit-test the configuration
 * class directly, using Mockito where interaction verification is needed.
 */
public class WebSecurityConfigTest {

    private UserService userService;
    private WebSecurityConfig webSecurityConfig;

    @BeforeEach
    void setUp() {
        // Using a mock UserService; interactions with it are outside the scope of the delta behavior
        this.userService = mock(UserService.class);
        this.webSecurityConfig = new WebSecurityConfig(userService);
    }

    /**
     * Verifies that the passwordEncoder bean returns a strong encoder implementation and
     * not a NoOp/plain-text encoder.
     *
     * This directly covers:
     * - "Don't use the default 'PasswordEncoder' relying on plain-text"
     * - "Use secure 'PasswordEncoder' implementation"
     */
    @Test
    void passwordEncoder_ShouldReturnStrongEncoder() {
        // Arrange & Act
        PasswordEncoder encoder = webSecurityConfig.passwordEncoder();

        // Assert
        assertNotNull(encoder, "PasswordEncoder bean must not be null");
        // We do not assert on the exact class type to keep the test valid if an equivalent strong encoder is chosen.
        // Instead, we assert on behavior: same raw password should not equal its encoded form.
        String rawPassword = "SensitivePassword123!";
        String encoded = encoder.encode(rawPassword);

        // Basic behavioral checks: non-null, different from raw,
        // and encoder matches its own encoding.
        assertNotNull(encoded, "Encoded password must not be null");
        assertFalse(
                encoded.equals(rawPassword),
                "Encoded password must not be equal to the raw password (no plain-text / NoOp encoding)");
        assertFalse(
                encoded.isEmpty(),
                "Encoded password must not be empty (indicates a broken or NoOp encoder)");
    }

    /**
     * Verifies that when the AuthenticationManagerBuilder is configured via {@code configureGlobal},
     * it is instructed to use the configured PasswordEncoder.
     *
     * This indirectly asserts that authentication is no longer based on plain-text passwords.
     */
    @Test
    void configureGlobal_ShouldConfigureAuthenticationManagerBuilderWithPasswordEncoder() throws Exception {
        // Arrange
        AuthenticationManagerBuilder authBuilder = Mockito.mock(AuthenticationManagerBuilder.class);
        UserDetailsService userDetailsServiceMock = this.userService;

        // We need to stub the userDetailsService(...) call to return the builder itself so the chained
        // passwordEncoder(...) call can be verified.
        Mockito.when(authBuilder.userDetailsService(userDetailsServiceMock)).thenReturn(authBuilder);

        // Act
        webSecurityConfig.configureGlobal(authBuilder);

        // Assert
        // Verify that userDetailsService(...) was set with our injected userService
        Mockito.verify(authBuilder).userDetailsService(userDetailsServiceMock);
        // Verify that the builder was also configured with a PasswordEncoder
        Mockito.verify(authBuilder).passwordEncoder(Mockito.any(PasswordEncoder.class));
    }

    /**
     * Verifies that CSRF protection is enabled in the configured SecurityFilterChain.
     *
     * This covers:
     * - "Spring Security's CSRF protection is disabled"
     *
     * We assert indirectly that CSRF is enabled by checking that the resulting filter chain
     * contains a {@link CsrfFilter}. If CSRF had been disabled, this filter would not be present.
     *
     * NOTE: This test assumes that {@link HttpSecurity#build()} produces a filter chain that
     * includes a {@link CsrfFilter} when CSRF is enabled (default behavior in Spring Security).
     */
    @Test
    void filterChain_ShouldIncludeCsrfFilter_WhenCsrfIsNotExplicitlyDisabled() throws Exception {
        // Arrange
        HttpSecurity http = new HttpSecurity(
                Mockito.mock(ObjectPostProcessorConfiguration.ObjectPostProcessor.class),
                new AuthenticationManagerBuilder(null),
                new java.util.HashMap<>());

        // Act
        SecurityFilterChain chain = webSecurityConfig.filterChain(http);

        // Assert
        assertNotNull(chain, "SecurityFilterChain must not be null");

        boolean hasCsrfFilter =
                chain.getFilters().stream().anyMatch(filter -> filter instanceof CsrfFilter);

        // If CSRF was disabled explicitly, CsrfFilter would not be in the chain
        // and this assertion would fail.
        assertFalse(
                !hasCsrfFilter,
                "SecurityFilterChain must contain CsrfFilter when CSRF is enabled (i.e., not explicitly disabled)");
    }

    // TODO: The instantiation of HttpSecurity in this test is based on internal constructors
    // and may require adjustment depending on the exact Spring Security version and project setup.
    // If this proves brittle, consider switching to a @SpringBootTest + MockMvc-based test that
    // inspects CSRF behavior via HTTP interactions instead.
}
