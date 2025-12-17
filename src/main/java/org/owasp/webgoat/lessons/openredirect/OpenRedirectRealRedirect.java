/*
 * SPDX-FileCopyrightText: Copyright © 2025 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.openredirect;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import java.util.regex.Pattern; // Added import

/**
 * Provides a real 302 redirect for experimentation separate from assignment scoring.
 */
@Controller
public class OpenRedirectRealRedirect {

  // Define a pattern for valid internal relative paths.
  // This example allows paths starting with / and containing alphanumeric, /, -, _, . characters.
  // It explicitly disallows external URLs or paths starting with //, http, etc.
  private static final Pattern SAFE_REDIRECT_PATH_PATTERN = Pattern.compile("^/[a-zA-Z0-9/\\-_.]*$"); // Added for validation

  @GetMapping("/OpenRedirect/realRedirect")
  public ModelAndView real(@RequestParam("url") String url) {
    // Validate the URL to prevent open redirect
    if (isValidRedirectUrl(url)) { // Added validation
      return new ModelAndView("redirect:" + url);
    } else {
      // Redirect to a safe default page if the provided URL is invalid
      return new ModelAndView("redirect:/home"); // Safe fallback
    }
  }

  /**
   * Validates if a given URL is safe for redirection.
   * For this example, only relative paths starting with '/' are considered safe.
   * Absolute URLs or URLs containing '..' for path traversal are rejected.
   * A more comprehensive solution might involve a whitelist of allowed domains.
   */
  private boolean isValidRedirectUrl(String url) {
    if (url == null || url.trim().isEmpty()) {
      return false;
    }
    // Reject URLs that start with "//" (protocol-relative) or contain a scheme (http/https)
    if (url.startsWith("//") || url.contains("://")) {
      return false;
    }
    // Reject paths containing path traversal sequences
    if (url.contains("..") || url.contains("%2e%2e")) {
      return false;
    }
    // Only allow paths that start with a single '/' and match the safe pattern
    return SAFE_REDIRECT_PATH_PATTERN.matcher(url).matches();
  }
}
