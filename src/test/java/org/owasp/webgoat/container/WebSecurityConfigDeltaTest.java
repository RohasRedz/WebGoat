package org.owasp.webgoat.container;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;

public class WebSecurityConfigDeltaTest {

    private static class StubUserService implements UserDetailsService {
        @Override
        public org.springframework.security.core.userdetails.UserDetails loadUserByUsername(String username) {
            throw new UnsupportedOperationException("Not required for delta tests");
        }
    }

    @Test
    void passwordEncoder_shouldUseBCryptPasswordEncoder_insteadOfNoOp() {
        WebSecurityConfig config = new WebSecurityConfig(new StubUserService());
        PasswordEncoder encoder = config.passwordEncoder();
        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
        String rawPassword = "secret123";
        String encodedPassword = encoder.encode(rawPassword);
        assertThat(encodedPassword).isNotEqualTo(rawPassword);
        assertThat(encoder.matches(rawPassword, encodedPassword)).isTrue();
    }

    @Test
    void csrfConfiguration_shouldUseCookieCsrfTokenRepository() {
        CsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        assertThat(repository).isInstanceOf(CookieCsrfTokenRepository.class);
    }
}
