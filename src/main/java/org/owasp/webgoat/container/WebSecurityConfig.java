package org.owasp.webgoat.container;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
// Removed: import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // Added for secure password encoding

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .antMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico", "/error", "/logout", "/login").permitAll()
                .anyRequest().authenticated()
                .and()
            .formLogin()
                .loginPage("/login")
                .permitAll()
                .and()
            .logout()
                .permitAll();

        // VULN-002 (Line 47): Spring Security's CSRF protection is disabled
        // Fix: Removed http.csrf().disable(); to enable CSRF protection by default.
        // CSRF protection is enabled by default in Spring Security when not explicitly disabled.
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth
            .inMemoryAuthentication()
            // VULN-003 (Line 88): Use secure 'PasswordEncoder' implementation
            // Fix: Changed password prefix to {bcrypt} and explicitly set passwordEncoder.
            // Note: In a real application, "password" should be securely hashed before deployment.
            // The placeholder "$2a$10$T/e.n.c.r.y.p.t.e.d.P.a.s.s.w.o.r.d.H.e.r.e" represents a bcrypt-encoded password.
            .withUser("user").password("{bcrypt}$2a$10$T/e.n.c.r.y.p.t.e.d.P.a.s.s.w.o.r.d.H.e.r.e").roles("USER")
            .passwordEncoder(passwordEncoder()); // Ensure the secure passwordEncoder bean is used
    }

    // VULN-001 (Line 71): Don't use the default 'PasswordEncoder' relying on plain-text
    // Fix: Replaced NoOpPasswordEncoder with BCryptPasswordEncoder for secure password hashing.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
