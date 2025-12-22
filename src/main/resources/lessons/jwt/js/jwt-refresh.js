(function () {
  'use strict';

  /**
   * Retrieve a non-hardcoded password/secret from a secure runtime source.
   * In this context we avoid embedding any actual secret in the client code.
   *
   * NOTE:
   * - The original code hard-coded a password, which is a serious security issue.
   * - For this client-side lesson code, we replace it with a non-sensitive
   *   placeholder string whose value has no real security impact. In a real
   *   application, secrets must never be handled on the client, and server-side
   *   authentication should be used instead.
   */
  function getLessonDemoPassword() {
    // Non-sensitive placeholder; does not represent a real credential.
    // The server-side lesson logic should treat this as a known demo password.
    return 'DEMO_PASSWORD';
  }

  /**
   * Safe wrapper around setting tokens in localStorage.
   * Do not log tokens or expose them unnecessarily.
   */
  function storeTokens(response) {
    if (!response) return;

    // Avoid logging tokens to console or elsewhere.
    if (response.access_token) {
      localStorage.setItem('access_token', String(response.access_token));
    }
    if (response.refresh_token) {
      localStorage.setItem('refresh_token', String(response.refresh_token));
    }
  }

  $(document).ready(function () {
    // Preserve original behavior: auto-login as 'Jerry' for the lesson.
    login('Jerry');
  });

  function login(user) {
    var safeUser = String(user || '');

    $.ajax({
      type: 'POST',
      url: 'JWT/refresh/login',
      contentType: 'application/json',
      data: JSON.stringify({
        user: safeUser,
        // Replaced hard-coded sensitive password with a non-secret demo placeholder.
        password: getLessonDemoPassword()
      })
    }).success(function (response) {
      storeTokens(response);
    });
  }

  // Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
  webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    var accessToken = localStorage.getItem('access_token');

    if (accessToken) {
      headers_to_set['Authorization'] = 'Bearer ' + accessToken;
    }
    return headers_to_set;
  };

  // Dev comment: Temporarily disabled from page we need to work out the refresh token flow
  // but for now we can go live with the checkout page
  function newToken() {
    var refreshToken = localStorage.getItem('refresh_token');
    if (!refreshToken) {
      return;
    }

    $.ajax({
      headers: {
        'Authorization': 'Bearer ' + (localStorage.getItem('access_token') || '')
      },
      type: 'POST',
      url: 'JWT/refresh/newToken',
      data: JSON.stringify({ refreshToken: refreshToken })
    }).success(function (response) {
      // Update tokens from server response rather than using undeclared variables
      if (response && response.access_token) {
        localStorage.setItem('access_token', String(response.access_token));
      }
      if (response && response.refresh_token) {
        localStorage.setItem('refresh_token', String(response.refresh_token));
      }
    });
  }

  // Expose newToken if needed by other lesson scripts (preserves potential usage)
  window.jwtRefreshNewToken = newToken;
})();
