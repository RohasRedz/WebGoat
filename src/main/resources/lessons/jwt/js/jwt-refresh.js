(function () {
    'use strict';

    var WEBGOAT_JWT_CONFIG = window.WEBGOAT_JWT_CONFIG || {};
    var JWT_PASSWORD = typeof WEBGOAT_JWT_CONFIG.password === 'string'
        ? WEBGOAT_JWT_CONFIG.password
        : null;

    function getJwtPassword() {
        // In a real application, this value must come from a secure backend or runtime config,
        // never hard-coded. Here we avoid embedding the secret directly in source.
        if (!JWT_PASSWORD) {
            // Fail closed: do not attempt login with an undefined or insecure password.
            throw new Error('JWT password is not configured securely on the client.');
        }
        return JWT_PASSWORD;
    }

    function login(user) {
        var password;
        try {
            password = getJwtPassword();
        } catch (e) {
            // Do not log the password or any sensitive data. Only log high-level info if needed.
            if (window.console && typeof window.console.error === 'function') {
                console.error('JWT login aborted due to missing secure password configuration.');
            }
            return;
        }

        $.ajax({
            type: 'POST',
            url: 'JWT/refresh/login',
            contentType: 'application/json',
            data: JSON.stringify({ user: user, password: password })
        }).done(function (response) {
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

    $(document).ready(function () {
        login('Jerry');
    });

    // Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
    window.webgoat = window.webgoat || {};
    window.webgoat.customjs = window.webgoat.customjs || {};
    window.webgoat.customjs.addBearerToken = function () {
        var headers_to_set = {};
        var token = localStorage.getItem('access_token');
        if (typeof token === 'string' && token.length > 0) {
            headers_to_set.Authorization = 'Bearer ' + token;
        }
        return headers_to_set;
    };

    // Dev comment: Temporarily disabled from page we need to work out the refresh token flow but
    // for now we can go live with the checkout page
    function newToken() {
        var refreshToken = localStorage.getItem('refresh_token');
        if (!refreshToken) {
            return;
        }

        $.ajax({
            headers: {
                Authorization: 'Bearer ' + localStorage.getItem('access_token')
            },
            type: 'POST',
            url: 'JWT/refresh/newToken',
            data: JSON.stringify({ refreshToken: refreshToken })
        }).done(function (response) {
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
