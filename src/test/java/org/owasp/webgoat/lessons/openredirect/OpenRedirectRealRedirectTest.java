package org.owasp.webgoat.lessons.openredirect;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Delta tests for the OpenRedirectRealRedirect controller.
 *
 * Focus: Verify that the open redirect vulnerability is fixed:
 *  - Only internal URLs (starting with "/") are allowed as redirect targets.
 *  - Invalid or external URLs are redirected to the safe default "/welcome.mvc".
 */
class OpenRedirectRealRedirectTest {

    private final OpenRedirectRealRedirect controller = new OpenRedirectRealRedirect();

    @Test
    @DisplayName("Should allow redirect only to internal paths starting with '/'")
    void internalUrlIsAllowed() {
        // Arrange
        String internalUrl = "/internal/page";

        // Act
        ModelAndView mav = controller.real(internalUrl);

        // Assert
        assertThat(mav.getViewName())
                .as("Internal URLs starting with '/' should be used as redirect targets")
                .isEqualTo("redirect:" + internalUrl);
    }

    @Test
    @DisplayName("Should redirect external URLs to safe default '/welcome.mvc'")
    void externalUrlIsRejectedAndRedirectsToSafePage() {
        // Arrange
        String externalUrl = "http://evil.com/malicious";

        // Act
        ModelAndView mav = controller.real(externalUrl);

        // Assert
        assertThat(mav.getViewName())
                .as("External URLs must not be used directly as redirect targets")
                .isEqualTo("redirect:/welcome.mvc");
    }

    @Test
    @DisplayName("Should redirect malformed or null URLs to safe default '/welcome.mvc'")
    void malformedOrNullUrlRedirectsToSafePage() {
        // Arrange
        String malformedUrl = "not-starting-with-slash";

        // Act
        ModelAndView mavMalformed = controller.real(malformedUrl);
        ModelAndView mavNull = controller.real(null);

        // Assert
        assertThat(mavMalformed.getViewName())
                .as("Malformed URLs must not be used directly as redirect targets")
                .isEqualTo("redirect:/welcome.mvc");

        assertThat(mavNull.getViewName())
                .as("Null URLs must not be used directly as redirect targets")
                .isEqualTo("redirect:/welcome.mvc");
    }
}
