/*
 * SPDX-FileCopyrightText: Copyright © 2025 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.openredirect;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Provides a real 302 redirect for experimentation separate from assignment scoring.
 */
@Controller
public class OpenRedirectRealRedirect {

  @GetMapping("/OpenRedirect/realRedirect")
  public ModelAndView real(@RequestParam("url") String url) {
    // Validate the redirect URL to prevent open redirect vulnerabilities.
    // Only allow redirects to internal paths starting with '/'.
    if (url != null && url.startsWith("/")) {
      return new ModelAndView(new RedirectView(url, true));
    }
    // Default to a safe internal page or return an error if validation fails.
    // For WebGoat, we'll redirect to the lesson's base path or a safe default.
    return new ModelAndView(new RedirectView("/OpenRedirect", true));
  }
}
