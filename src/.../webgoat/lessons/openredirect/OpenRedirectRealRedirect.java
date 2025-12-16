package org.owasp.webgoat.lessons.openredirect;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OpenRedirectRealRedirect {

    @GetMapping("/open-redirect/real-redirect")
    public RedirectView realRedirect(@RequestParam String url, RedirectAttributes redirectAttributes) {
        // Fixed: Validate redirect target to prevent Open Redirect
        // Only allow relative paths starting with '/'
        if (url != null && url.startsWith("/") && !url.startsWith("//") && !url.contains("://")) {
            return new RedirectView(url);
        } else {
            // Redirect to a safe default page or show an error
            redirectAttributes.addFlashAttribute("error", "Invalid redirect URL provided.");
            return new RedirectView("/welcome.mvc");
        }
    }
}
