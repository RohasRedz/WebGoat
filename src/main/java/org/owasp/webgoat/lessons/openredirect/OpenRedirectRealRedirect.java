/*
 * SPDX-FileCopyrightText: Copyright © 2025 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.openredirect;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * Provides a real 302 redirect for experimentation separate from assignment scoring.
 */
@Controller
public class OpenRedirectRealRedirect {

  @GetMapping("/OpenRedirect/realRedirect")
  public ModelAndView real(@RequestParam("url") String url) {
    // Vulnerability: Open Redirect (CWE-601) - 'url' parameter used directly in redirect without validation.
    // Fix: Validate the 'url' parameter to ensure it points to an internal path
    // or redirect to a safe default if it's an external or invalid URL.
    if (url != null && url.startsWith("/")) {
      // Only allow redirects to internal paths within the application.
      return new ModelAndView("redirect:" + url);
    } else {
      // For external or invalid URLs, redirect to a safe default page.
      // This prevents attackers from redirecting users to malicious sites.
      return new ModelAndView("redirect:/"); // Redirect to application root or a safe default page
    }
  }
}
