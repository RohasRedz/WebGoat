package org.owasp.webgoat.lessons.openredirect;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

public class OpenRedirectRealRedirectTest {

    private final OpenRedirectRealRedirect controller = new OpenRedirectRealRedirect();

    @Test
    void real_withInternalPathShouldRedirectToThatPath() {
        String internal = "/internal/page";

        ModelAndView mv = controller.real(internal);

        assertThat(mv.getViewName()).isEqualTo("redirect:" + internal);
    }

    @Test
    void real_withExternalAbsoluteUrlShouldRedirectToSafeDefault() {
        String external = "http://evil.com";

        ModelAndView mv = controller.real(external);

        assertThat(mv.getViewName()).isNotEqualTo("redirect:" + external);
        assertThat(mv.getViewName()).isEqualTo("redirect:/welcome.mvc");
    }

    @Test
    void real_withMalformedUrlShouldRedirectToSafeDefault() {
        String malformed = "://not-a-valid-uri";

        ModelAndView mv = controller.real(malformed);

        assertThat(mv.getViewName()).isEqualTo("redirect:/welcome.mvc");
    }
}
