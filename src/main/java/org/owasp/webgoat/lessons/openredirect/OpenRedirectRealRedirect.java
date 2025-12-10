/*
 * SPDX-FileCopyrightText: Copyright © 2025 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.openredirect;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView; // SVCF-324: Added for safe redirects

/**
 * Provides a real 302 redirect for experimentation separate from assignment scoring.
 */
@Controller
public class OpenRedirectRealRedirect {

  @GetMapping("/OpenRedirect/realRedirect")
  public ModelAndView real(@RequestParam("url") String url) {
    // SVCF-324: Remediation for Open Redirect - validate redirect target
    // This is a simplified example. In a real application, a more robust whitelist
    // or internal path mapping mechanism should be used.
    if (url != null && url.startsWith("/") && !url.contains("//") && !url.contains("\\ ")) {
      return new ModelAndView("redirect:" + url);
    } else {
      // Redirect to a safe default page or return an error
      return new ModelAndView("redirect:/home"); // Redirect to a safe default
    }
  }
}
