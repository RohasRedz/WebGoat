$(document).ready(function () {
  login('Jerry');
});

function login(user) {
  /**
   * SECURITY FIX:
   * Remove hard-coded password from client-side code.
   * The backend should handle authentication or provide a demo/non-sensitive token flow.
   * Here, we send a clearly non-secret placeholder value that cannot be used as a real credential.
   */
  var demoPassword = 'not-a-real-password';

  $.ajax({
    type: 'POST',
    url: 'JWT/refresh/login',
    contentType: 'application/json',
    data: JSON.stringify({ user: user, password: demoPassword })
  }).success(function (response) {
    // Still store tokens client-side as the application originally does,
    // but do NOT log or expose these tokens anywhere else.
    localStorage.setItem('access_token', response['access_token']);
    localStorage.setItem('refresh_token', response['refresh_token']);
  });
}

// Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
  var headers_to_set = {};
  headers_to_set['Authorization'] = 'Bearer ' + localStorage.getItem('access_token');
  return headers_to_set;
};

// Dev comment: Temporarily disabled from page we need to work out the refresh token flow
// but for now we can go live with the checkout page
function newToken() {
  // NOTE: do not log or expose refresh tokens; we only read from localStorage.
  var refreshToken = localStorage.getItem('refresh_token');

  $.ajax({
    headers: {
      Authorization: 'Bearer ' + localStorage.getItem('access_token')
    },
    type: 'POST',
    url: 'JWT/refresh/newToken',
    contentType: 'application/json',
    data: JSON.stringify({ refreshToken: refreshToken })
  }).success(function (response) {
    // Use the newly returned tokens instead of undefined variables.
    // Preserve original behavior while avoiding reliance on undeclared identifiers.
    if (response && response['access_token']) {
      localStorage.setItem('access_token', response['access_token']);
    }
    if (response && response['refresh_token']) {
      localStorage.setItem('refresh_token', response['refresh_token']);
    }
  });
}
