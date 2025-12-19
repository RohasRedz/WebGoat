package org.owasp.webgoat.lessons.openredirect;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

/**
 * Delta tests for OpenRedirectRealRedirect focusing ONLY on the changed behavior:
 * 1) Redirect is allowed only for whitelisted internal paths.
 * 2) External / malformed / non-whitelisted paths are redirected to the safe default "/".
 * 3) Redirect still occurs successfully for allowed paths.
 */
class OpenRedirectRealRedirectTest {

    private final OpenRedirectRealRedirect controller = new OpenRedirectRealRedirect();

    @Test
    @DisplayName("Should redirect to whitelisted internal path /welcome.mvc")
    void shouldRedirectToWhitelistedWelcome() {
        // Act
        ModelAndView mav = controller.real("/welcome.mvc");

        // Assert
        assertThat(mav.getViewName())
                .as("Whitelisted path /welcome.mvc should be used as redirect target")
                .isEqualTo("redirect:/welcome.mvc");
    }

    @Test
    @DisplayName("Should redirect to whitelisted internal path /login")
    void shouldRedirectToWhitelistedLogin() {
        // Act
        ModelAndView mav = controller.real("/login");

        // Assert
        assertThat(mav.getViewName())
                .as("Whitelisted path /login should be used as redirect target")
                .isEqualTo("redirect:/login");
    }

    @Test
    @DisplayName("Should redirect to whitelisted internal root path /")
    void shouldRedirectToWhitelistedRoot() {
        // Act
        ModelAndView mav = controller.real("/");

        // Assert
        assertThat(mav.getViewName())
                .as("Whitelisted root path / should be used as redirect target")
                .isEqualTo("redirect:/");
    }

    @Test
    @DisplayName("Should redirect to safe default for external http URL")
    void shouldRedirectExternalHttpToSafeDefault() {
        // Act
        ModelAndView mav = controller.real("http://evil.com/phish");

        // Assert
        assertThat(mav.getViewName())
                .as("External http URL must NOT be used as redirect target")
                .isEqualTo("redirect:/");
    }

    @Test
    @DisplayName("Should redirect to safe default for external https URL")
    void shouldRedirectExternalHttpsToSafeDefault() {
        // Act
        ModelAndView mav = controller.real("https://malicious.example.com");

        // Assert
        assertThat(mav.getViewName())
                .as("External https URL must NOT be used as redirect target")
                .isEqualTo("redirect:/");
    }

    @Test
    @DisplayName("Should redirect to safe default for any URL containing ://")
    void shouldRedirectAnyUrlWithSchemeToSafeDefault() {
        // Act
        ModelAndView mav = controller.real("custom-scheme://whatever");

        // Assert
        assertThat(mav.getViewName())
                .as("Any URL containing :// must NOT be used as redirect target")
                .isEqualTo("redirect:/");
    }

    @Test
    @DisplayName("Should redirect to safe default when URL is null")
    void shouldRedirectNullToSafeDefault() {
        // Act
        ModelAndView mav = controller.real(null);

        // Assert
        assertThat(mav.getViewName())
                .as("Null URL must be normalized to safe default")
                .isEqualTo("redirect:/");
    }

    @Test
    @DisplayName("Should redirect to safe default when URL is empty")
    void shouldRedirectEmptyToSafeDefault() {
        // Act
        ModelAndView mav = controller.real("");

        // Assert
        assertThat(mav.getViewName())
                .as("Empty URL must be normalized to safe default")
                .isEqualTo("redirect:/");
    }

    @Test
    @DisplayName("Should redirect to safe default when URL is not an absolute path")
    void shouldRedirectNonAbsolutePathToSafeDefault() {
        // Act
        ModelAndView mav = controller.real("relative/path");

        // Assert
        assertThat(mav.getViewName())
                .as("Non-absolute path must NOT be used as redirect target")
                .isEqualTo("redirect:/");
    }

    @Test
    @DisplayName("Should redirect to safe default when URL is internal but not whitelisted")
    void shouldRedirectNonWhitelistedInternalPathToSafeDefault() {
        // Act
        ModelAndView mav = controller.real("/admin/secret");

        // Assert
        assertThat(mav.getViewName())
                .as("Non-whitelisted internal path must NOT be used as redirect target")
                .isEqualTo("redirect:/");
    }
}
