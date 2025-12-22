/* eslint-disable no-undef */
$(document).ready(function () {
    login('Jerry');
});

function login(user) {
    var safePassword = 'placeholder-password';

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({ user: user, password: safePassword })
    }).success(
        function (response) {
            if (response && typeof response === 'object') {
                if (typeof response.access_token === 'string') {
                    localStorage.setItem('access_token', response.access_token);
                }
                if (typeof response.refresh_token === 'string') {
                    localStorage.setItem('refresh_token', response.refresh_token);
                }
            }
        }
    );
}

webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    var accessToken = localStorage.getItem('access_token');
    if (typeof accessToken === 'string' && accessToken.length > 0) {
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
        headers: webgoat.customjs.addBearerToken(),
        type: 'POST',
        url: 'JWT/refresh/newToken',
        contentType: "application/json",
        data: JSON.stringify({ refreshToken: refreshToken })
    }).success(
        function (response) {
            if (response && typeof response === 'object') {
                if (typeof response.access_token === 'string') {
                    localStorage.setItem('access_token', response.access_token);
                }
                if (typeof response.refresh_token === 'string') {
                    localStorage.setItem('refresh_token', response.refresh_token);
                }
            }
        }
    );
}
