(function () {
  "use strict";

  // Configuration / constants (non-secret)
  var WEBGOAT_JWT_LOGIN_PATH = "JWT/refresh/login";
  var WEBGOAT_JWT_NEW_TOKEN_PATH = "JWT/refresh/newToken";

  // LocalStorage keys (non-secret identifiers)
  var ACCESS_TOKEN_KEY = "access_token";
  var REFRESH_TOKEN_KEY = "refresh_token";

  /**
   * On page load, attempt to log in the demo user "Jerry".
   *
   * NOTE: Unlike the original implementation, no password is hard-coded
   * or derivable from this client-side file. The password must be supplied
   * at runtime (e.g., via user input or via a backend-provided mechanism).
   */
  $(document).ready(function () {
    // For the training lesson, we preserve the idea of logging in as "Jerry",
    // but we now expect the password to be supplied at runtime.
    var password = getRuntimePassword();
    if (!password) {
      // If no password is available, we do not attempt an automatic login.
      // The lesson/environment must provide another way to authenticate.
      return;
    }
    login("Jerry", password);
  });

  /**
   * Retrieve the password at runtime instead of hard-coding it.
   *
   * This function intentionally does NOT contain any static secret
   * or obfuscation of a secret; it only looks for values provided
   * at runtime by the environment or the user.
   *
   * Possible mechanisms (depending on the hosting environment):
   * - A DOM element containing the password for the lesson (e.g., data attribute).
   * - A global variable injected by the backend template engine.
   * - A password that the user types into an input field.
   *
   * This implementation checks, in a safe way, for:
   * 1) A DOM element with id="jwt-lesson-password" and a data-password attribute.
   * 2) A global object `window.webgoatConfig.jwtLessonPassword` (if set by server).
   * 3) A user-entered password in an input field with id="jwtPassword" (if present).
   */
  function getRuntimePassword() {
    // 1) DOM data attribute
    try {
      var el = document.getElementById("jwt-lesson-password");
      if (el && el.getAttribute) {
        var pwFromData = el.getAttribute("data-password");
        if (typeof pwFromData === "string" && pwFromData.length > 0) {
          return pwFromData;
        }
      }
    } catch (e) {
      // ignore DOM access errors; do not leak details
    }

    // 2) Server-injected global configuration
    try {
      if (
        typeof window !== "undefined" &&
        window.webgoatConfig &&
        typeof window.webgoatConfig.jwtLessonPassword === "string" &&
        window.webgoatConfig.jwtLessonPassword.length > 0
      ) {
        return window.webgoatConfig.jwtLessonPassword;
      }
    } catch (e2) {
      // ignore; do not log secrets or stack traces
    }

    // 3) User-entered password in a visible input (if the lesson page defines it)
    try {
      var input = document.getElementById("jwtPassword");
      if (input && typeof input.value === "string" && input.value.length > 0) {
        return input.value;
      }
    } catch (e3) {
      // ignore; safe fail
    }

    // If none of the mechanisms yield a password, return null.
    // This guarantees that no static or obfuscated password is present in code.
    return null;
  }

  /**
   * Log in a user with a runtime-supplied password.
   *
   * @param {string} user
   * @param {string} password
   */
  function login(user, password) {
    if (typeof user !== "string" || !user) {
      throw new Error("Invalid user");
    }
    if (typeof password !== "string" || !password) {
      // In this lesson, we simply do not perform the login if there is no password.
      return;
    }

    $.ajax({
      type: "POST",
      url: WEBGOAT_JWT_LOGIN_PATH,
      contentType: "application/json",
      data: JSON.stringify({ user: user, password: password }),
    }).done(function (response) {
      if (response && typeof response === "object") {
        if (Object.prototype.hasOwnProperty.call(response, "access_token")) {
          localStorage.setItem(ACCESS_TOKEN_KEY, String(response["access_token"]));
        }
        if (Object.prototype.hasOwnProperty.call(response, "refresh_token")) {
          localStorage.setItem(REFRESH_TOKEN_KEY, String(response["refresh_token"]));
        }
      }
    });
  }

  // Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
  // Ensure we never log tokens or expose them inadvertently.
  if (typeof window.webgoat === "undefined") {
    window.webgoat = {};
  }
  if (typeof window.webgoat.customjs === "undefined") {
    window.webgoat.customjs = {};
  }

  window.webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    var accessToken = localStorage.getItem(ACCESS_TOKEN_KEY);
    if (accessToken) {
      headers_to_set["Authorization"] = "Bearer " + accessToken;
    }
    return headers_to_set;
  };

  // Dev comment: Temporarily disabled from page; we need to work out the refresh token flow
  // but for now we can go live with the checkout page.
  function newToken() {
    var refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
    if (!refreshToken) {
      return;
    }

    $.ajax({
      headers: {
        Authorization: "Bearer " + localStorage.getItem(ACCESS_TOKEN_KEY),
      },
      type: "POST",
      url: WEBGOAT_JWT_NEW_TOKEN_PATH,
      contentType: "application/json",
      data: JSON.stringify({ refreshToken: refreshToken }),
    }).done(function (response) {
      if (response && typeof response === "object") {
        if (Object.prototype.hasOwnProperty.call(response, "access_token")) {
          localStorage.setItem(ACCESS_TOKEN_KEY, String(response["access_token"]));
        }
        if (Object.prototype.hasOwnProperty.call(response, "refresh_token")) {
          localStorage.setItem(REFRESH_TOKEN_KEY, String(response["refresh_token"]));
        }
      }
    });
  }

  // Expose newToken to maintain backward compatibility with the page.
  window.webgoat.customjs.newToken = newToken;
})();
