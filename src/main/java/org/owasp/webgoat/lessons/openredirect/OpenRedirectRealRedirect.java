/*
 * SPDX-FileCopyrightText: Copyright © 2025 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.openredirect;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Provides a real 302 redirect for experimentation separate from assignment scoring.
 */
@Controller
public class OpenRedirectRealRedirect {

  private static final String DEFAULT_REDIRECT_URL = "/";

  @GetMapping("/OpenRedirect/realRedirect")
  public ModelAndView real(@RequestParam("url") String url) {
    String safeUrl = validateRedirectUrl(url);
    return new ModelAndView("redirect:" + safeUrl);
  }

  private String validateRedirectUrl(String url) {
    if (url == null || url.trim().isEmpty()) {
      return DEFAULT_REDIRECT_URL;
    }

    // Allow relative paths within the application
    if (url.startsWith("/") && !url.startsWith("//")) {
      return url;
    }

    try {
      URL parsedUrl = new URL(url);
      // Whitelist schemes and hosts
      boolean isAllowedScheme = parsedUrl.getProtocol().equalsIgnoreCase("http") ||
                                parsedUrl.getProtocol().equalsIgnoreCase("https");
      boolean isAllowedHost = parsedUrl.getHost().equalsIgnoreCase("localhost") ||
                              parsedUrl.getHost().equals("127.0.0.1");

      if (isAllowedScheme && isAllowedHost) {
        return url;
      }
    } catch (MalformedURLException e) {
      // URL is malformed, treat as unsafe
    }

    // If not relative and not whitelisted absolute, redirect to default
    return DEFAULT_REDIRECT_URL;
  }
}
