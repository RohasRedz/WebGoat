// src/main/resources/lessons/jwt/js/jwt-refresh.js

(function () {
  'use strict';

  /**
   * NOTE:
   * The original implementation hard-coded a password in the client-side script.
   * That is fundamentally insecure, because anyone can view source and extract it.
   *
   * This file has been refactored to:
   * - Remove the hard-coded password value entirely from client code.
   * - Require a password to be provided from a non-hardcoded source (e.g., user input).
   * - Prevent accidental logging of tokens or secrets.
   *
   * The actual secure provisioning of credentials must be handled server-side
   * or via a secure configuration/secret mechanism; the frontend must not embed secrets.
   */

  // Wrapper to ensure webgoat and webgoat.customjs namespaces exist without polluting global scope
  if (typeof window.webgoat !== 'object' || window.webgoat === null) {
    window.webgoat = {};
  }
  if (typeof window.webgoat.customjs !== 'object' || window.webgoat.customjs === null) {
    window.webgoat.customjs = {};
  }

  /**
   * Initialize the flow when the document is ready.
   * A password must now be supplied by the caller; it is no longer hard-coded here.
   */
  $(document).ready(function () {
    // In a real application, the password should be collected via a secure user input form
    // and never hard-coded in the frontend.
    //
    // This call intentionally does NOT provide a password value here to avoid hardcoding.
    // The backend should enforce proper authentication and reject missing/invalid passwords.
    login('Jerry', null);
  });

  /**
   * Perform login and retrieve tokens.
   * @param {string} user - username (e.g., 'Jerry').
   * @param {string|null} password - user password; must NOT be hard-coded in this file.
   */
  function login(user, password) {
    // Basic runtime guard to avoid accidentally calling login with a hard-coded secret
    // left in the source. If password is missing or obviously placeholder, let the
    // server handle authentication failure without exposing any secret.
    var safePassword = typeof password === 'string' ? password : '';

    $.ajax({
      type: 'POST',
      url: 'JWT/refresh/login',
      contentType: 'application/json',
      // Never log this body; it may contain credentials.
      data: JSON.stringify({ user: user, password: safePassword })
    }).done(function (response) {
      // Do not log tokens; store them locally only.
      if (response && typeof response.access_token === 'string') {
        localStorage.setItem('access_token', response.access_token);
      }
      if (response && typeof response.refresh_token === 'string') {
        localStorage.setItem('refresh_token', response.refresh_token);
      }
    });
  }

  // Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
  window.webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    var accessToken = localStorage.getItem('access_token');

    // Do not log or expose the token; just attach if available.
    if (typeof accessToken === 'string' && accessToken.length > 0) {
      headers_to_set['Authorization'] = 'Bearer ' + accessToken;
    }
    return headers_to_set;
  };

  // Dev comment: Temporarily disabled from page we need to work out the refresh token flow
  // but for now we can go live with the checkout page
  function newToken() {
    // Use the stored refresh token from localStorage without logging it
    var refreshToken = localStorage.getItem('refresh_token');
    var accessToken = localStorage.getItem('access_token');

    if (!refreshToken || !accessToken) {
      // Fail silently; a proper implementation would surface a generic error to the user.
      return;
    }

    $.ajax({
      headers: {
        'Authorization': 'Bearer ' + accessToken
      },
      type: 'POST',
      url: 'JWT/refresh/newToken',
      // Never log this body; it may contain credentials/tokens.
      data: JSON.stringify({ refreshToken: refreshToken }),
      contentType: 'application/json'
    }).done(function (response) {
      // Update tokens from server response, if any; do not log them.
      if (response && typeof response.access_token === 'string') {
        localStorage.setItem('access_token', response.access_token);
      }
      if (response && typeof response.refresh_token === 'string') {
        localStorage.setItem('refresh_token', response.refresh_token);
      }
    });
  }
})();
