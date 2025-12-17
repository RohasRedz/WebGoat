// Assumed package based on source file path; adjust if needed.
package org.owasp.webgoat.lessons.openredirect;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Delta tests for OpenRedirectRealRedirect focused on:
 * - Only allowing internal paths (starting with '/').
 * - Falling back to a safe internal URL for invalid/external URLs.
 */
class OpenRedirectRealRedirectTest {

    @Test
    void real_shouldRedirectToInternalPathWhenUrlStartsWithSlash() {
        // Arrange
        OpenRedirectRealRedirect controller = new OpenRedirectRealRedirect();
        String internalPath = "/internal/page";

        // Act
        ModelAndView mav = controller.real(internalPath);

        // Assert
        assertThat(mav.getView()).isInstanceOf(RedirectView.class);
        RedirectView view = (RedirectView) mav.getView();
        assertThat(view.getUrl()).isEqualTo(internalPath);
    }

    @Test
    void real_shouldFallbackToSafeUrlWhenUrlIsExternal() {
        // Arrange
        OpenRedirectRealRedirect controller = new OpenRedirectRealRedirect();
        String externalUrl = "https://evil.com/phish";

        // Act
        ModelAndView mav = controller.real(externalUrl);

        // Assert
        assertThat(mav.getView()).isInstanceOf(RedirectView.class);
        RedirectView view = (RedirectView) mav.getView();
        // Must not redirect to the attacker-controlled external URL
        assertThat(view.getUrl()).isEqualTo("/OpenRedirect");
    }

    @Test
    void real_shouldFallbackToSafeUrlWhenUrlIsNullOrEmpty() {
        // Arrange
        OpenRedirectRealRedirect controller = new OpenRedirectRealRedirect();

        // Act
        ModelAndView mavNull = controller.real(null);
        ModelAndView mavEmpty = controller.real("");

        // Assert
        assertThat(mavNull.getView()).isInstanceOf(RedirectView.class);
        assertThat(((RedirectView) mavNull.getView()).getUrl()).isEqualTo("/OpenRedirect");

        assertThat(mavEmpty.getView()).isInstanceOf(RedirectView.class);
        assertThat(((RedirectView) mavEmpty.getView()).getUrl()).isEqualTo("/OpenRedirect");
    }
}
