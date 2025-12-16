// Delta unit test for OpenRedirectRealRedirect.java
// Assumed package based on resolved_file_path; adjust if actual package differs.
package org.owasp.webgoat.lessons.openredirect;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

class OpenRedirectRealRedirectTest {

    @Test
    void realShouldAllowRelativeInternalPathStartingWithSlash() throws Exception {
        // Arrange
        OpenRedirectRealRedirect controller = new OpenRedirectRealRedirect();
        String safeUrl = "/welcome.mvc";

        // Act
        ModelAndView mav = controller.real(safeUrl);

        // Assert
        assertThat(mav).isNotNull();
        assertThat(mav.getViewName()).isEqualTo("redirect:" + safeUrl);
    }

    @Test
    void realShouldRedirectToSafeDefaultWhenRelativePathDoesNotStartWithSlash() throws Exception {
        // Arrange
        OpenRedirectRealRedirect controller = new OpenRedirectRealRedirect();
        String unsafeUrl = "http://evil.com/relative";

        // Act
        ModelAndView mav = controller.real(unsafeUrl);

        // Assert
        // Because the string is absolute, the host is not whitelisted and should
        // fall back to the safe default welcome page.
        assertThat(mav).isNotNull();
        assertThat(mav.getViewName()).isEqualTo("redirect:/welcome.mvc");
    }

    @Test
    void realShouldRejectAbsoluteUrlWithNonWhitelistedHost() throws Exception {
        // Arrange
        OpenRedirectRealRedirect controller = new OpenRedirectRealRedirect();
        String externalUrl = "https://evil.com/";

        // Act
        ModelAndView mav = controller.real(externalUrl);

        // Assert
        // The fix enforces an allow-list; non-localhost hosts must be redirected
        // to the safe default.
        assertThat(mav.getViewName()).isEqualTo("redirect:/welcome.mvc");
    }

    @Test
    void realShouldAllowAbsoluteUrlWithWhitelistedHost() throws Exception {
        // Arrange
        OpenRedirectRealRedirect controller = new OpenRedirectRealRedirect();
        String localhostUrl = "http://localhost/path";

        // Act
        ModelAndView mav = controller.real(localhostUrl);

        // Assert
        // Host is whitelisted, so redirect is allowed.
        assertThat(mav.getViewName()).isEqualTo("redirect:" + localhostUrl);
    }
}
