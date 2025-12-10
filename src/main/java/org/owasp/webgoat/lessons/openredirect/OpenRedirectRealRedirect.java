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

  private static final String DEFAULT_REDIRECT_URL = "/welcome.mvc"; // Define a safe default redirect

  @GetMapping("/OpenRedirect/realRedirect")
  public ModelAndView real(@RequestParam("url") String url) {
    // Remediation: Validate the 'url' parameter to prevent open redirects.
    // Restrict redirects to relative paths starting with "/" and reject absolute/external URLs.
    if (url != null && url.startsWith("/") && !url.contains("://")) {
      return new ModelAndView("redirect:" + url);
    } else {
      // If validation fails, redirect to a safe default URL.
      return new ModelAndView(new RedirectView(DEFAULT_REDIRECT_URL));
    }
  }
}
