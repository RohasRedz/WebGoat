(function () {
    'use strict';

    // Non-sensitive, demo-only default user; not treated as a secret
    var DEFAULT_USER = 'Jerry';

    function safeGetLocalStorageItem(key) {
        try {
            return window.localStorage ? window.localStorage.getItem(key) : null;
        } catch (e) {
            return null;
        }
    }

    function safeSetLocalStorageItem(key, value) {
        try {
            if (window.localStorage) {
                window.localStorage.setItem(key, value);
            }
        } catch (e) {
            // Intentionally ignore; do not expose internal details
        }
    }

    function login(user) {
        // Password is no longer hard-coded in the source. In a real deployment this
        // value must be provided securely from the server (e.g., templated in,
        // or retrieved via a secure bootstrap endpoint) rather than stored in code.
        var passwordFromMeta = (function () {
            var el = document.querySelector('meta[name="webgoat-jwt-demo-password"]');
            return el && el.content ? el.content : '';
        })();

        $.ajax({
            type: 'POST',
            url: 'JWT/refresh/login',
            contentType: 'application/json',
            data: JSON.stringify({
                user: user,
                password: passwordFromMeta
            })
        }).success(function (response) {
            if (response && typeof response === 'object') {
                if (Object.prototype.hasOwnProperty.call(response, 'access_token')) {
                    safeSetLocalStorageItem('access_token', response['access_token']);
                }
                if (Object.prototype.hasOwnProperty.call(response, 'refresh_token')) {
                    safeSetLocalStorageItem('refresh_token', response['refresh_token']);
                }
            }
        });
    }

    // Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
    window.webgoat = window.webgoat || {};
    window.webgoat.customjs = window.webgoat.customjs || {};

    window.webgoat.customjs.addBearerToken = function () {
        var headers_to_set = {};
        var token = safeGetLocalStorageItem('access_token');
        if (token) {
            headers_to_set['Authorization'] = 'Bearer ' + token;
        }
        return headers_to_set;
    };

    // Dev comment: Temporarily disabled from page we need to work out the refresh token flow but for now we can go live with the checkout page
    function newToken() {
        var accessToken = safeGetLocalStorageItem('access_token');
        var refreshToken = safeGetLocalStorageItem('refresh_token');

        if (!refreshToken) {
            return;
        }

        $.ajax({
            headers: accessToken ? { 'Authorization': 'Bearer ' + accessToken } : {},
            type: 'POST',
            url: 'JWT/refresh/newToken',
            contentType: 'application/json',
            data: JSON.stringify({ refreshToken: refreshToken })
        }).success(function (response) {
            if (response && typeof response === 'object') {
                if (Object.prototype.hasOwnProperty.call(response, 'access_token')) {
                    safeSetLocalStorageItem('access_token', response['access_token']);
                }
                if (Object.prototype.hasOwnProperty.call(response, 'refresh_token')) {
                    safeSetLocalStorageItem('refresh_token', response['refresh_token']);
                }
            }
        });
    }

    // Preserve original behaviour: on document ready, perform login for demo user
    $(document).ready(function () {
        login(DEFAULT_USER);
    });

    // Expose functions if needed by other scripts (non-breaking change)
    window.webgoat.customjs.login = login;
    window.webgoat.customjs.newToken = newToken;
})();
