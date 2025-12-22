$(document).ready(function () {
  login('Jerry');
});

function login(user) {
  // Password is no longer hard-coded; it is expected to be injected at build-time
  // or configured via a non-checked-in configuration mechanism.
  // Here we read from a global config object populated server-side in the page,
  // which must NOT be committed with real secrets.
  var password =
    (window.webgoatConfig &&
      typeof window.webgoatConfig.jwtDemoPassword === 'string' &&
      window.webgoatConfig.jwtDemoPassword) ||
    '';

  $.ajax({
    type: 'POST',
    url: 'JWT/refresh/login',
    contentType: 'application/json',
    data: JSON.stringify({
      user: user,
      password: password,
    }),
  }).success(function (response) {
    // Store tokens; note that localStorage is used here for demo purposes.
    // In a real application, prefer HttpOnly, Secure cookies or secure storage.
    localStorage.setItem('access_token', response['access_token']);
    localStorage.setItem('refresh_token', response['refresh_token']);
  });
}

// Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
  var headers_to_set = {};
  var accessToken = localStorage.getItem('access_token');
  if (typeof accessToken === 'string' && accessToken.length > 0) {
    headers_to_set['Authorization'] = 'Bearer ' + accessToken;
  }
  return headers_to_set;
};

// Dev comment: Temporarily disabled from page we need to work out the refresh token flow but for now we can go live with the checkout page
function newToken() {
  var refreshToken = localStorage.getItem('refresh_token');
  if (!refreshToken) {
    return;
  }

  $.ajax({
    headers: {
      Authorization: 'Bearer ' + localStorage.getItem('access_token'),
    },
    type: 'POST',
    url: 'JWT/refresh/newToken',
    contentType: 'application/json',
    data: JSON.stringify({ refreshToken: refreshToken }),
  }).success(function (response) {
    // Expect server to return new tokens instead of undeclared variables
    if (response && response.access_token && response.refresh_token) {
      localStorage.setItem('access_token', response.access_token);
      localStorage.setItem('refresh_token', response.refresh_token);
    }
  });
}
