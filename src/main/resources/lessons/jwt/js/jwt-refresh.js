$(document).ready(function () {
  login('Jerry');
});

function login(user) {
  var password = window.WEBGOAT_JWT_REFRESH_PASSWORD;
  if (typeof password !== 'string' || !password.length) {
    // Fail securely if no configured secret is available
    // Do not log the actual password or any sensitive data
    throw new Error('JWT refresh password is not configured');
  }

  $.ajax({
    type: 'POST',
    url: 'JWT/refresh/login',
    contentType: 'application/json',
    data: JSON.stringify({ user: user, password: password }),
  }).success(function (response) {
    localStorage.setItem('access_token', response['access_token']);
    localStorage.setItem('refresh_token', response['refresh_token']);
  });
}

// Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
  var headers_to_set = {};
  headers_to_set['Authorization'] =
    'Bearer ' + localStorage.getItem('access_token');
  return headers_to_set;
};

// Dev comment: Temporarily disabled from page we need to work out the refresh token flow but for now we can go live with the checkout page
function newToken() {
  localStorage.getItem('refreshToken');
  $.ajax({
    headers: {
      Authorization: 'Bearer ' + localStorage.getItem('access_token'),
    },
    type: 'POST',
    url: 'JWT/refresh/newToken',
    data: JSON.stringify({
      refreshToken: localStorage.getItem('refresh_token'),
    }),
  }).success(function () {
    // NOTE: This relies on apiToken and refreshToken being set by the backend or surrounding script.
    // Ensure these values are never hardcoded and are treated as secrets.
    localStorage.setItem('access_token', apiToken);
    localStorage.setItem('refresh_token', refreshToken);
  });
}
