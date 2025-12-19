/*
 * SPDX-FileCopyrightText: Copyright © 2025 WebGoat authors
 * SPDX-License-Identifier: GPL-2.0-or-later
 */
package org.owasp.webgoat.lessons.openredirect;

import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * Provides a real 302 redirect for experimentation separate from assignment scoring.
 */
@Controller
public class OpenRedirectRealRedirect {

  private static final String DEFAULT_SAFE_REDIRECT = "/welcome.mvc";

  @GetMapping("/OpenRedirect/realRedirect")
  public ModelAndView real(@RequestParam("url") String url) {
    try {
      URI uri = new URI(url);

      if (uri.isAbsolute()) {
        return new ModelAndView("redirect:" + DEFAULT_SAFE_REDIRECT);
      } else if (url.startsWith("/")) {
        return new ModelAndView("redirect:" + url);
      } else {
        return new ModelAndView("redirect:" + DEFAULT_SAFE_REDIRECT);
      }
    } catch (URISyntaxException e) {
      return new ModelAndView("redirect:" + DEFAULT_SAFE_REDIRECT);
    }
  }
}
