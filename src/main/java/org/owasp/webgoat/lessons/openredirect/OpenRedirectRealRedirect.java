/*
 * SPDX-FileCopyrightText: Copyright © 2025 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.openredirect;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView; // Added for RedirectView

/**
 * Provides a real 302 redirect for experimentation separate from assignment scoring.
 */
@Controller
public class OpenRedirectRealRedirect {

  @GetMapping("/OpenRedirect/realRedirect")
  public ModelAndView real(@RequestParam("url") String url) {
    // Remediation: Validate redirect URL to prevent Open Redirect
    if (url == null || url.trim().isEmpty() || !url.startsWith("/")) {
      // Redirect to a safe default page or error page if the URL is not valid (e.g., not a relative path)
      return new ModelAndView("redirect:/welcome.mvc"); // Redirect to a safe, internal default
    }
    // Intentionally vulnerable: no validation
    return new ModelAndView("redirect:" + url);
  }
}
