/*
 * SPDX-FileCopyrightText: Copyright © 2025 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.openredirect;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Provides a real 302 redirect for experimentation separate from assignment scoring.
 */
@Controller
public class OpenRedirectRealRedirect {

  @GetMapping("/OpenRedirect/realRedirect")
  public ModelAndView real(@RequestParam("url") String url) {
    // Remediation: Validate the 'url' parameter to prevent Open Redirect
    try {
      URI uri = new URI(url);
      // Allow only relative paths (no scheme, no host, starts with /)
      if (uri.getScheme() == null && uri.getHost() == null && uri.getPath() != null && uri.getPath().startsWith("/")) {
        // Further sanitize path to prevent path traversal within relative paths
        String sanitizedPath = uri.normalize().getPath();
        if (sanitizedPath.contains("..") || sanitizedPath.contains("%2e%2e")) {
            return new ModelAndView("redirect:/"); // Redirect to a safe default if path traversal attempt
        }
        return new ModelAndView("redirect:" + sanitizedPath);
      }
    } catch (URISyntaxException e) {
      // Log the exception for debugging, but do not expose to user
      // Fall through to safe default redirect
    }
    // If validation fails or an exception occurs, redirect to a safe default page
    return new ModelAndView("redirect:/");
  }
}
