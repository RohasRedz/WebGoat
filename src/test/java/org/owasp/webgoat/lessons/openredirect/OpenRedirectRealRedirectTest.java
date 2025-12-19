package org.owasp.webgoat.lessons.openredirect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

/**
 * Delta tests for OpenRedirectRealRedirect focusing on redirect validation.
 *
 * These tests verify that:
 * - Internal paths are still honored.
 * - External URLs are no longer allowed and redirect to a safe default.
 */
class OpenRedirectRealRedirectTest {

    @Test
    @DisplayName("real should redirect to internal path when URL starts with slash")
    void realShouldRedirectToInternalPath() {
        // Arrange
        OpenRedirectRealRedirect controller = new OpenRedirectRealRedirect();

        // Act
        ModelAndView mav = controller.real("/home");

        // Assert
        assertEquals("redirect:/home", mav.getViewName(), "Internal path must remain directly redirectable");
    }

    @Test
    @DisplayName("real should redirect to safe default for external URLs")
    void realShouldRedirectToSafeDefaultForExternalUrl() {
        // Arrange
        OpenRedirectRealRedirect controller = new OpenRedirectRealRedirect();

        // Act
        ModelAndView mav = controller.real("http://evil.com");

        // Assert
        assertEquals("redirect:/welcome.mvc", mav.getViewName(),
                "External URL must not be used directly and should redirect to a safe default");
    }

    @Test
    @DisplayName("real should redirect to safe default for null or empty URL")
    void realShouldRedirectToSafeDefaultForNullOrEmpty() {
        // Arrange
        OpenRedirectRealRedirect controller = new OpenRedirectRealRedirect();

        // Act
        ModelAndView mavNull = controller.real(null);
        ModelAndView mavEmpty = controller.real("");

        // Assert
        assertEquals("redirect:/welcome.mvc", mavNull.getViewName(),
                "Null URL should redirect to safe default");
        assertEquals("redirect:/welcome.mvc", mavEmpty.getViewName(),
                "Empty URL should redirect to safe default");
    }
}
