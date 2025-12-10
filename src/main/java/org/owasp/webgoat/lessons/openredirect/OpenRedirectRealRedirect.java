/*
 * SPDX-FileCopyrightText: Copyright © 2025 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.openredirect;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import java.util.Set;
import java.util.HashSet;

/**
 * Provides a real 302 redirect for experimentation separate from assignment scoring.
 */
@Controller
public class OpenRedirectRealRedirect {

  // Define an allowlist of safe internal paths for redirection
  private static final Set<String> ALLOWED_REDIRECT_PATHS = new HashSet<>();

  static {
    ALLOWED_REDIRECT_PATHS.add("/welcome.mvc");
    ALLOWED_REDIRECT_PATHS.add("/login");
    // Add other safe internal paths as needed for the application
    // For example, if there are other lesson specific redirects, they should be added here.
  }

  @GetMapping("/OpenRedirect/realRedirect")
  public ModelAndView real(@RequestParam("url") String url) {
    // Validate the 'url' parameter against a whitelist to prevent open redirects.
    // If the URL is not safe, redirect to a default safe page.
    if (url != null && url.startsWith("/") && ALLOWED_REDIRECT_PATHS.contains(url)) {
      return new ModelAndView("redirect:" + url);
    } else {
      // If the provided URL is not in the allowlist or is not a safe relative path,
      // redirect to a safe default page to prevent open redirect vulnerability.
      return new ModelAndView("redirect:/login"); // Redirect to login page as a safe default
    }
  }
}
