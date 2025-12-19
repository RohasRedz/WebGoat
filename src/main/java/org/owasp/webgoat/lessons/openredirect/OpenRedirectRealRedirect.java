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
    // FIX: Validate redirect URL to prevent Open Redirect vulnerability
    if (url != null && url.startsWith("/")) {
      // If the URL starts with '/', it's considered an internal path
      return new ModelAndView("redirect:" + url);
    } else {
      // For external or invalid URLs, redirect to a safe default page
      return new ModelAndView("redirect:/welcome.mvc"); // Redirect to a safe default
    }
  }
}
