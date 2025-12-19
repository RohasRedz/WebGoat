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

  // FIX: Define a whitelist of allowed internal redirect paths
  // This set contains only paths that are explicitly known and safe for redirection.
  private static final Set<String> ALLOWED_REDIRECT_PATHS = new HashSet<>();

  static {
    // Populate the whitelist with known safe internal paths.
    // For WebGoat, common safe internal redirects might include:
    ALLOWED_REDIRECT_PATHS.add("/welcome.mvc");
    ALLOWED_REDIRECT_PATHS.add("/login");
    ALLOWED_REDIRECT_PATHS.add("/"); // The application's root path
    // Add other specific internal paths as needed, e.g., "/some/safe/internal/page"
  }

  @GetMapping("/OpenRedirect/realRedirect")
  public ModelAndView real(@RequestParam("url") String url) {
    // FIX: Validate redirect target against a whitelist to prevent open redirect.
    // If the provided URL is null, empty, or not a safe internal path,
    // redirect to a hardcoded safe default.
    if (url == null || url.isEmpty()) {
      return new ModelAndView("redirect:/"); // Default to root if no URL provided
    }

    // 1. Reject external URLs: Check if the URL attempts to redirect to an external domain.
    // This prevents redirects to arbitrary malicious sites.
    if (url.startsWith("http://") || url.startsWith("https://") || url.contains("://")) {
        return new ModelAndView("redirect:/"); // Redirect to a safe default page
    }

    // 2. Ensure the URL is an absolute path within the application.
    // This prevents relative path tricks or redirects to non-standard locations.
    if (!url.startsWith("/")) {
        return new ModelAndView("redirect:/"); // Redirect to a safe default page
    }

    // 3. Check if the provided internal path is in our allow-list.
    // Only allow redirection to explicitly approved internal paths.
    if (ALLOWED_REDIRECT_PATHS.contains(url)) {
      return new ModelAndView("redirect:" + url);
    } else {
      // If the URL is not in the allow-list, redirect to a safe default.
      return new ModelAndView("redirect:/");
    }
  }
}
