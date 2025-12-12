package org.owasp.webgoat.lessons.openredirect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

public class RealRedirectControllerTest {

    private final RealRedirectController controller = new RealRedirectController();

    @Test
    void real_shouldRedirectToGivenInternalPath_whenUrlStartsWithSlash() {
        String url = "/internal/page";
        ModelAndView mav = controller.real(url);
        assertEquals("redirect:" + url, mav.getViewName());
    }

    @Test
    void real_shouldRedirectToSafeDefault_whenUrlIsExternalHttp() {
        String url = "http://evil.com/phish";
        ModelAndView mav = controller.real(url);
        assertEquals("redirect:/welcome.mvc", mav.getViewName());
    }

    @Test
    void real_shouldRedirectToSafeDefault_whenUrlIsExternalHttps() {
        String url = "https://example.com";
        ModelAndView mav = controller.real(url);
        assertEquals("redirect:/welcome.mvc", mav.getViewName());
    }

    @Test
    void real_shouldRedirectToSafeDefault_whenUrlIsNull() {
        String url = null;
        ModelAndView mav = controller.real(url);
        assertEquals("redirect:/welcome.mvc", mav.getViewName());
    }

    @Test
    void real_shouldRedirectToSafeDefault_whenUrlIsRelativeWithoutLeadingSlash() {
        String url = "relative/path";
        ModelAndView mav = controller.real(url);
        assertEquals("redirect:/welcome.mvc", mav.getViewName());
    }
}
