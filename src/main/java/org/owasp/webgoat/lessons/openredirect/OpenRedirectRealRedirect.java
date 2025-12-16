/*
 * SPDX-FileCopyrightText: Copyright © 2025 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.openredirect;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView; // Added import
import java.net.URI; // Added import
import java.net.URISyntaxException; // Added import

/**
 * Provides a real 302 redirect for experimentation separate from assignment scoring.
 */
@Controller
public class OpenRedirectRealRedirect {

  private static final String ALLOWED_REDIRECT_HOST = "localhost"; // Example: Whitelist allowed host

  @GetMapping("/OpenRedirect/realRedirect")
  public ModelAndView real(@RequestParam("url") String url) throws URISyntaxException {
    // Fixed: Validate redirect target against a whitelist and ensure it's a safe internal path
    URI redirectUri = new URI(url);
    if (redirectUri.isAbsolute()) {
      // For absolute URLs, check against a whitelist of allowed hosts
      if (!ALLOWED_REDIRECT_HOST.equalsIgnoreCase(redirectUri.getHost())) {
        // Redirect to a safe default or error page if host is not whitelisted
        return new ModelAndView("redirect:/welcome.mvc"); // Safe default redirect
      }
    } else {
      // For relative URLs, ensure they start with '/' to prevent scheme/host manipulation
      if (!url.startsWith("/")) {
        return new ModelAndView("redirect:/welcome.mvc"); // Safe default redirect
      }
    }
    return new ModelAndView("redirect:" + url);
  }
}
