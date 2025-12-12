package org.owasp.webgoat.lessons.openredirect;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

/**
 * Delta tests for OpenRedirectRealRedirect.
 *
 * Focus: validateRedirectUrl behavior and its effect on the redirect target:
 * - Allows relative paths starting with "/" but not protocol-relative URLs ("//").
 * - Allows only http/https URLs pointing to localhost/127.0.0.1.
 * - Falls back to "/" for unsafe or malformed URLs.
 *
 * NOTE: validateRedirectUrl is private; here we assert behavior via the public real() method by
 * inspecting the resulting redirect view name.
 */
public class OpenRedirectRealRedirectTest {

    @Test
    void real_shouldAllowSafeRelativePath() {
        // Arrange
        OpenRedirectRealRedirect controller = new OpenRedirectRealRedirect();
        String userSuppliedUrl = "/some/internal/path";

        // Act
        ModelAndView mav = controller.real(userSuppliedUrl);

        // Assert
        assertThat(mav.getViewName()).isEqualTo("redirect:" + userSuppliedUrl);
    }

    @Test
    void real_shouldRejectProtocolRelativeUrlAndFallbackToRoot() {
        // Arrange
        OpenRedirectRealRedirect controller = new OpenRedirectRealRedirect();
        String userSuppliedUrl = "//evil.example.com/phish";

        // Act
        ModelAndView mav = controller.real(userSuppliedUrl);

        // Assert
        assertThat(mav.getViewName()).isEqualTo("redirect:/");
    }

    @Test
    void real_shouldAllowWhitelistedAbsoluteLocalhostHttpUrl() {
        // Arrange
        OpenRedirectRealRedirect controller = new OpenRedirectRealRedirect();
        String userSuppliedUrl = "http://localhost/internal";

        // Act
        ModelAndView mav = controller.real(userSuppliedUrl);

        // Assert
        assertThat(mav.getViewName()).isEqualTo("redirect:" + userSuppliedUrl);
    }

    @Test
    void real_shouldAllowWhitelistedAbsoluteLocalhostHttpsUrl() {
        // Arrange
        OpenRedirectRealRedirect controller = new OpenRedirectRealRedirect();
        String userSuppliedUrl = "https://127.0.0.1/secure";

        // Act
        ModelAndView mav = controller.real(userSuppliedUrl);

        // Assert
        assertThat(mav.getViewName()).isEqualTo("redirect:" + userSuppliedUrl);
    }

    @Test
    void real_shouldRejectExternalDomainAndFallbackToRoot() {
        // Arrange
        OpenRedirectRealRedirect controller = new OpenRedirectRealRedirect();
        String userSuppliedUrl = "https://evil.com/attack";

        // Act
        ModelAndView mav = controller.real(userSuppliedUrl);

        // Assert
        assertThat(mav.getViewName()).isEqualTo("redirect:/");
    }

    @Test
    void real_shouldFallbackToRootOnMalformedUrl() {
        // Arrange
        OpenRedirectRealRedirect controller = new OpenRedirectRealRedirect();
        String userSuppliedUrl = "http:///bad-url"; // malformed URL

        // Act
        ModelAndView mav = controller.real(userSuppliedUrl);

        // Assert
        assertThat(mav.getViewName()).isEqualTo("redirect:/");
    }

    @Test
    void real_shouldFallbackToRootOnEmptyOrNull() {
        // Arrange
        OpenRedirectRealRedirect controller = new OpenRedirectRealRedirect();

        // Act
        ModelAndView mavEmpty = controller.real("");
        ModelAndView mavSpaces = controller.real("   ");

        // Assert
        assertThat(mavEmpty.getViewName()).isEqualTo("redirect:/");
        assertThat(mavSpaces.getViewName()).isEqualTo("redirect:/");
    }
}
