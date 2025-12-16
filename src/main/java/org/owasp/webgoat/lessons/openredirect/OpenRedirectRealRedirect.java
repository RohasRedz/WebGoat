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
    // Remediation: Validate the 'url' parameter to prevent open redirects.
    // Only allow relative paths starting with '/' or a whitelisted domain.
    // For this lesson, we enforce relative paths.
    if (url != null && url.startsWith("/")) {
      return new ModelAndView("redirect:" + url);
    } else {
      // Redirect to a safe default page or an error page if the URL is invalid
      return new ModelAndView("redirect:/welcome.mvc"); // Redirect to a safe default page
    }
  }
}
