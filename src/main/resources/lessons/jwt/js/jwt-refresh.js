(function () {
  'use strict';

  // NOTE:
  // For security, do NOT embed real passwords or secrets in client-side code.
  // This demo uses a placeholder to avoid hard-coded secrets being used in practice.
  var DEMO_STATIC_PASSWORD = '***REDACTED_DEMO_PASSWORD***';

  $(document).ready(function () {
    // For the WebGoat lesson, this still demonstrates the flow without exposing a real secret.
    login('Jerry');
  });

  function login(user) {
    // Validate the user input in case this is ever parameterized in the lesson
    var safeUser =
      typeof user === 'string' ? user.replace(/[^a-zA-Z0-9_.-]/g, '') : '';

    if (!safeUser) {
      // Fallback to a safe default username used for the lesson
      safeUser = 'Jerry';
    }

    $.ajax({
      type: 'POST',
      url: 'JWT/refresh/login',
      contentType: 'application/json',
      // Do not send actual secrets from client-side code; this is a demo-only placeholder value.
      data: JSON.stringify({ user: safeUser, password: DEMO_STATIC_PASSWORD }),
    }).success(function (response) {
      if (response && typeof response === 'object') {
        if (response.access_token) {
          localStorage.setItem('access_token', response.access_token);
        }
        if (response.refresh_token) {
          localStorage.setItem('refresh_token', response.refresh_token);
        }
      }
    });
  }

  // Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
  webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    var accessToken = localStorage.getItem('access_token');

    if (typeof accessToken === 'string' && accessToken.trim() !== '') {
      headers_to_set.Authorization = 'Bearer ' + accessToken;
    }

    return headers_to_set;
  };

  // Dev comment: Temporarily disabled from page we need to work out the refresh token flow
  // but for now we can go live with the checkout page
  function newToken() {
    var refreshToken = localStorage.getItem('refresh_token');
    var accessToken = localStorage.getItem('access_token');

    if (
      typeof refreshToken !== 'string' ||
      refreshToken.trim() === '' ||
      typeof accessToken !== 'string' ||
      accessToken.trim() === ''
    ) {
      // Tokens are not present; do not attempt refresh
      return;
    }

    $.ajax({
      headers: {
        Authorization: 'Bearer ' + accessToken,
      },
      type: 'POST',
      url: 'JWT/refresh/newToken',
      data: JSON.stringify({ refreshToken: refreshToken }),
      contentType: 'application/json',
    }).success(function (response) {
      if (response && typeof response === 'object') {
        if (response.access_token) {
          localStorage.setItem('access_token', response.access_token);
        }
        if (response.refresh_token) {
          localStorage.setItem('refresh_token', response.refresh_token);
        }
      }
    });
  }
})();
