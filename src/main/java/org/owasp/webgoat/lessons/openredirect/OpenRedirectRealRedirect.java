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
  public RedirectView real(@RequestParam("url") String url) {
    // Validate the URL to prevent open redirects
    if (url != null && url.startsWith("/") && !url.contains("://")) {
      // Only allow internal redirects
      return new RedirectView(url);
    } else {
      // Redirect to a safe default page or error page if the URL is invalid
      return new RedirectView("/"); // Redirect to home page or an error page
    }
  }
}
