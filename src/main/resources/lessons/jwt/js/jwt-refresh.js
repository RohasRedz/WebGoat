$(document).ready(function () {
  login('Jerry');
});

function login(user) {
  // NOTE: Password is no longer hardcoded in source.
  // It should be provided by the backend or a secure configuration channel,
  // not visible in client-side code.
  $.ajax({
    type: 'POST',
    url: 'JWT/refresh/login',
    contentType: 'application/json',
    data: JSON.stringify({ user: user }),
  }).success(function (response) {
    if (response && typeof response.access_token === 'string') {
      localStorage.setItem('access_token', response.access_token);
    }
    if (response && typeof response.refresh_token === 'string') {
      localStorage.setItem('refresh_token', response.refresh_token);
    }
  });
}

// Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs = webgoat.customjs || {};
webgoat.customjs.addBearerToken = function () {
  var headers_to_set = {};
  var token = localStorage.getItem('access_token');
  if (typeof token === 'string' && token.length > 0) {
    headers_to_set.Authorization = 'Bearer ' + token;
  }
  return headers_to_set;
};

// Dev comment: Temporarily disabled from page we need to work out the refresh token flow
// but for now we can go live with the checkout page
function newToken() {
  var currentAccessToken = localStorage.getItem('access_token');
  var currentRefreshToken = localStorage.getItem('refresh_token');

  if (!currentRefreshToken) {
    // No refresh token available; nothing to do.
    return;
  }

  $.ajax({
    headers: {
      Authorization: currentAccessToken ? 'Bearer ' + currentAccessToken : undefined,
    },
    type: 'POST',
    url: 'JWT/refresh/newToken',
    contentType: 'application/json',
    data: JSON.stringify({ refreshToken: currentRefreshToken }),
  }).success(function (response) {
    // Expect the API to safely return new tokens. Do not rely on global variables.
    if (response && typeof response.access_token === 'string') {
      localStorage.setItem('access_token', response.access_token);
    }
    if (response && typeof response.refresh_token === 'string') {
      localStorage.setItem('refresh_token', response.refresh_token);
    }
  });
}
