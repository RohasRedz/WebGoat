(function () {
  'use strict';

  var LOGIN_PLACEHOLDER_PASSWORD = 'webgoat-demo-password';

  $(document).ready(function () {
    login('Jerry');
  });

  function login(user) {
    $.ajax({
      type: 'POST',
      url: 'JWT/refresh/login',
      contentType: 'application/json',
      data: JSON.stringify({ user: user, password: LOGIN_PLACEHOLDER_PASSWORD }),
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

  if (typeof webgoat === 'undefined') {
    window.webgoat = {};
  }
  if (!webgoat.customjs) {
    webgoat.customjs = {};
  }

  webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    var accessToken = localStorage.getItem('access_token');
    if (accessToken) {
      headers_to_set['Authorization'] = 'Bearer ' + accessToken;
    }
    return headers_to_set;
  };

  function newToken() {
    var refreshToken = localStorage.getItem('refresh_token');
    if (!refreshToken) {
      return;
    }

    $.ajax({
      headers: {
        Authorization: 'Bearer ' + (localStorage.getItem('access_token') || ''),
      },
      type: 'POST',
      url: 'JWT/refresh/newToken',
      contentType: 'application/json',
      data: JSON.stringify({ refreshToken: refreshToken }),
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

  webgoat.customjs.newToken = newToken;
})();
