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
    // Remediation for Open Redirect: Validate the 'url' parameter.
    // Only allow relative paths within this application to prevent redirection to arbitrary external sites.
    // If the URL is not a valid relative path, redirect to a safe default page.

    if (url != null) {
      // Normalize the URL to prevent path manipulation (e.g., double slashes)
      String normalizedUrl = url.trim();

      // Check if the URL starts with a single '/' (indicating a relative path within the application)
      // This prevents redirection to external domains or protocols like 'javascript:'
      if (normalizedUrl.startsWith("/") && !normalizedUrl.startsWith("//")) {
        // If it's a valid relative path, proceed with the redirect
        return new ModelAndView("redirect:" + normalizedUrl);
      }
    }
    // If the URL is null, empty, not a relative path, or starts with double slashes,
    // redirect to a safe default page to prevent open redirect vulnerabilities.
    return new ModelAndView("redirect:/welcome.mvc");
  }
}
