// Delta_UnitTest_Agent
// NOTE: Package inferred from production class; adjust if project structure differs.
package org.owasp.webgoat.container;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.util.ReflectionTestUtils;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

/**
 * Delta unit tests for WebSecurityConfig (Jira: SVCF-655).
 *
 * Focus only on changed behavior:
 *  - CSRF protection must now be enabled (no longer disabled).
 *  - PasswordEncoder bean must use BCrypt (no longer NoOp/plaintext).
 */
public class WebSecurityConfigDeltaTest {

    /**
     * Minimal stub of HttpSecurity to verify that CSRF is not disabled.
     * We cannot easily assert the internals of the built SecurityFilterChain
     * without a full Spring context, so we verify the configuration path
     * indirectly by checking that the configuration lambda is not disabling CSRF.
     *
     * This test focuses on the regression: previously CSRF was explicitly disabled
     * via csrf(csrf -> csrf.disable()).
     */
    @Test
    @DisplayName("CSRF must not be explicitly disabled on HttpSecurity")
    void csrfIsNotDisabled() throws Exception {
        WebSecurityConfig config = new WebSecurityConfig(null /* UserService is not used in filterChain */);

        // Create a real HttpSecurity but avoid starting a full context.
        HttpSecurity http = new HttpSecurity(null, null, List.of(), null, null, null);

        SecurityFilterChain chain = config.filterChain(http);

        // Assert that a SecurityFilterChain was created
        assertThat(chain).isNotNull();

        // Indirect verification:
        // - The previous implementation used csrf(csrf -> csrf.disable()).
        // - Now it uses csrf(Customizer.withDefaults()).
        // There is no public "isDisabled" on CSRF, but we can assert that
        // the CsrfConfigurer object on HttpSecurity is not marked as disabled.
        CsrfConfigurer<HttpSecurity> csrfConfigurer =
                (CsrfConfigurer<HttpSecurity>) ReflectionTestUtils.getField(http, "csrf");
        // If configuration had been csrf(csrf -> csrf.disable()), the internal 'disable' flag
        // would be set. We can assert that it is not explicitly disabled.
        assertThat(csrfConfigurer).isNotNull();
        Object disabled = ReflectionTestUtils.getField(csrfConfigurer, "disabled");
        assertThat(disabled)
                .as("CSRF should not be explicitly disabled anymore")
                .isNotEqualTo(Boolean.TRUE);
    }

    /**
     * Verifies that the PasswordEncoder bean now returns a BCryptPasswordEncoder
     * (or at least a PasswordEncoder whose implementation is BCryptPasswordEncoder),
     * ensuring plaintext passwords are no longer used.
     */
    @Test
    @DisplayName("PasswordEncoder bean must use BCrypt implementation")
    void passwordEncoderUsesBcrypt() {
        WebSecurityConfig config = new WebSecurityConfig(null /* UserService not required here */);

        PasswordEncoder encoder = config.passwordEncoder();

        assertThat(encoder)
                .as("PasswordEncoder bean should not be null")
                .isNotNull();

        // Ensure it is not the old insecure NoOpPasswordEncoder
        assertThat(encoder.getClass().getName())
                .as("NoOpPasswordEncoder must not be used anymore")
                .doesNotContain("NoOpPasswordEncoder");

        // Ensure BCrypt is used
        assertThat(encoder)
                .as("PasswordEncoder should be an instance of BCryptPasswordEncoder")
                .isInstanceOf(BCryptPasswordEncoder.class);

        // Additional behavioral assertion: encoding must be non-plaintext and verify matches()
        String rawPassword = "SensitiveP@ssw0rd";
        String encoded = encoder.encode(rawPassword);

        assertThat(encoded)
                .as("Encoded password must differ from raw password to avoid plaintext storage")
                .isNotEqualTo(rawPassword);

        assertThat(encoder.matches(rawPassword, encoded))
                .as("PasswordEncoder must correctly verify BCrypt-hashed password")
                .isTrue();
    }
}
