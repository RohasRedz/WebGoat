// Delta_UnitTest_Agent
// Package inferred from source file path; adjust if actual package differs.
package org.owasp.webgoat.lessons.openredirect;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

import java.lang.reflect.Field;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Delta tests for OpenRedirectRealRedirect focusing only on the changed behavior:
 * - Validation of user-controlled redirect URL via isValidRedirectUrl
 * - Restricting redirects to safe internal paths
 * - Falling back to a safe default (/home) for invalid URLs
 *
 * Original (vulnerable) behavior:
 *   return new ModelAndView("redirect:" + url);
 *
 * Updated behavior (secured):
 *   - Accept only validated internal paths (e.g. "/foo/bar")
 *   - Reject external URLs (http://, https://, protocol-relative //)
 *   - Reject path traversal (.., %2e%2e)
 *   - Redirect invalid inputs to "/home"
 */
class OpenRedirectRealRedirectTest {

    private final OpenRedirectRealRedirect controller = new OpenRedirectRealRedirect();

    @Nested
    @DisplayName("URL validation and redirect behavior")
    class RedirectValidationTests {

        @Test
        @DisplayName("Should allow safe internal relative paths")
        void shouldAllowSafeInternalRelativePaths() {
            // Arrange
            String safeUrl = "/lessons/openredirect";

            // Act
            ModelAndView mav = controller.real(safeUrl);

            // Assert
            assertThat(mav.getViewName()).isEqualTo("redirect:" + safeUrl);
        }

        @Test
        @DisplayName("Should reject absolute http URL and redirect to safe default")
        void shouldRejectExternalHttpUrlAndUseSafeFallback() {
            // Arrange
            String externalUrl = "http://evil.com/phish";

            // Act
            ModelAndView mav = controller.real(externalUrl);

            // Assert
            // Previously would have been "redirect:http://evil.com/phish"
            assertThat(mav.getViewName()).isEqualTo("redirect:/home");
        }

        @Test
        @DisplayName("Should reject protocol-relative URL and redirect to safe default")
        void shouldRejectProtocolRelativeUrlAndUseSafeFallback() {
            // Arrange
            String externalUrl = "//evil.com/phish";

            // Act
            ModelAndView mav = controller.real(externalUrl);

            // Assert
            assertThat(mav.getViewName()).isEqualTo("redirect:/home");
        }

        @Test
        @DisplayName("Should reject path traversal attempts and redirect to safe default")
        void shouldRejectPathTraversalAndUseSafeFallback() {
            // Arrange
            String traversalUrl = "/../../etc/passwd";

            // Act
            ModelAndView mav = controller.real(traversalUrl);

            // Assert
            assertThat(mav.getViewName()).isEqualTo("redirect:/home");
        }

        @Test
        @DisplayName("Should reject encoded path traversal attempts and redirect to safe default")
        void shouldRejectEncodedTraversalAndUseSafeFallback() {
            // Arrange
            String encodedTraversal = "/foo/%2e%2e/%2e%2e/secret";

            // Act
            ModelAndView mav = controller.real(encodedTraversal);

            // Assert
            assertThat(mav.getViewName()).isEqualTo("redirect:/home");
        }

        @Test
        @DisplayName("Should reject null or blank URLs and redirect to safe default")
        void shouldRejectNullOrBlankAndUseSafeFallback() {
            // Null
            ModelAndView mavNull = controller.real(null);
            assertThat(mavNull.getViewName()).isEqualTo("redirect:/home");

            // Blank
            ModelAndView mavBlank = controller.real("   ");
            assertThat(mavBlank.getViewName()).isEqualTo("redirect:/home");
        }

        @Test
        @DisplayName("Safe regex pattern should only allow paths starting with slash and safe characters")
        void safeRedirectPatternShouldOnlyAllowExpectedCharacters() throws Exception {
            // This test introspects SAFE_REDIRECT_PATH_PATTERN to assert it is restrictive.
            Field patternField = OpenRedirectRealRedirect.class.getDeclaredField("SAFE_REDIRECT_PATH_PATTERN");
            patternField.setAccessible(true);
            Pattern pattern = (Pattern) patternField.get(null);

            assertThat(pattern.matcher("/valid/path-123_ABC.xyz").matches()).isTrue();
            assertThat(pattern.matcher("/").matches()).isTrue();

            // Disallowed because of spaces, query, or not starting with '/'
            assertThat(pattern.matcher("no/leading/slash").matches()).isFalse();
            assertThat(pattern.matcher("/has space").matches()).isFalse();
            assertThat(pattern.matcher("/path?query=1").matches()).isFalse();
        }
    }
}
