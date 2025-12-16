// TODO: Adjust the package to match the actual source package if different.
package org.owasp.webgoat.lessons.openredirect;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.assertj.core.api.Assertions.assertThat;

class OpenRedirectRealRedirectTest {

    @Test
    @DisplayName("realRedirect should allow only safe relative URLs starting with '/'")
    void realRedirect_allowsOnlySafeRelativeUrls() {
        // Arrange
        OpenRedirectRealRedirect controller = new OpenRedirectRealRedirect();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        // Act
        RedirectView view = controller.realRedirect("/internal/page", redirectAttributes);

        // Assert - secure behavior: still allows safe internal redirects
        assertThat(view.getUrl()).isEqualTo("/internal/page");
        assertThat(redirectAttributes.getFlashAttributes()).doesNotContainKey("error");
    }

    @Test
    @DisplayName("realRedirect should reject absolute URLs and redirect to safe default")
    void realRedirect_rejectsAbsoluteUrlAndUsesSafeDefault() {
        // Arrange
        OpenRedirectRealRedirect controller = new OpenRedirectRealRedirect();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        // Act
        RedirectView view = controller.realRedirect("http://malicious.example.com", redirectAttributes);

        // Assert - the vulnerability is fixed: open redirect to external domain is not allowed
        assertThat(view.getUrl()).isEqualTo("/welcome.mvc");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("error");
    }

    @Test
    @DisplayName("realRedirect should reject protocol-relative URLs and redirect to safe default")
    void realRedirect_rejectsProtocolRelativeUrl() {
        // Arrange
        OpenRedirectRealRedirect controller = new OpenRedirectRealRedirect();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        // Act
        RedirectView view = controller.realRedirect("//evil.com", redirectAttributes);

        // Assert
        assertThat(view.getUrl()).isEqualTo("/welcome.mvc");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("error");
    }

    @Test
    @DisplayName("realRedirect should reject URLs containing '://' and redirect to safe default")
    void realRedirect_rejectsUrlsContainingSchemeSeparator() {
        // Arrange
        OpenRedirectRealRedirect controller = new OpenRedirectRealRedirect();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        // Act
        RedirectView view = controller.realRedirect("/path/with/http://segment", redirectAttributes);

        // Assert
        assertThat(view.getUrl()).isEqualTo("/welcome.mvc");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("error");
    }
}
