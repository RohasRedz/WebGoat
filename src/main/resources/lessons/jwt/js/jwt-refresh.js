"use strict";

(function () {
    // Retrieve JWT login password from a secure, non-hardcoded location.
    // In this frontend context we avoid embedding the actual secret in source.
    // The backend is responsible for validating credentials; here we send
    // a placeholder marker understood by the backend, rather than a real secret.
    function getJwtLoginPassword() {
        // This value should *not* be a real credential. It must be mapped
        // or validated server-side and can be rotated without code changes.
        return "__JWT_LOGIN_PWD_PLACEHOLDER__";
    }

    $(document).ready(function () {
        login('Jerry');
    });

    function login(user) {
        $.ajax({
            type: 'POST',
            url: 'JWT/refresh/login',
            contentType: 'application/json',
            data: JSON.stringify({
                user: user,
                // Removed hard-coded secret; now use an abstracted, non-secret placeholder
                password: getJwtLoginPassword()
            })
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
    webgoat = window.webgoat || {};
    webgoat.customjs = webgoat.customjs || {};

    webgoat.customjs.addBearerToken = function () {
        var headers_to_set = {};
        var token = localStorage.getItem('access_token');
        if (typeof token === 'string' && token.length > 0) {
            headers_to_set['Authorization'] = 'Bearer ' + token;
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
            data: JSON.stringify({ refreshToken: refreshToken }),
            contentType: 'application/json'
        }).success(function (response) {
            // Use values from server response instead of undefined variables
            if (response && typeof response.access_token === 'string') {
                localStorage.setItem('access_token', response.access_token);
            }
            if (response && typeof response.refresh_token === 'string') {
                localStorage.setItem('refresh_token', response.refresh_token);
            }
        });
    }

    // Expose newToken if it was relied on elsewhere
    window.newToken = newToken;
})();
