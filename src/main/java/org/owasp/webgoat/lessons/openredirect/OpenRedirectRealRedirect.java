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
    // Remediation: Validate redirect targets against a whitelist or enforce internal paths
    // For this example, we enforce that the URL must start with '/' for internal redirects.
    // In a real application, a more comprehensive whitelist of allowed domains would be used.
    if (!url.startsWith("/")) {
      // Redirect to a safe default page if the URL is not an internal path
      return new ModelAndView("redirect:/");
    }
    return new ModelAndView("redirect:" + url);
  }
}
