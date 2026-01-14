(function () {
    'use strict';

    var JWT_REFRESH_PWD_KEY = 'WEBGOAT_JWT_REFRESH_PWD';

    function getRefreshPassword() {
        // Prefer a non-hardcoded secret from a configurable source if available
        if (window.webgoat && window.webgoat.config && typeof window.webgoat.config.get === 'function') {
            var cfgPwd = window.webgoat.config.get(JWT_REFRESH_PWD_KEY);
            if (typeof cfgPwd === 'string' && cfgPwd.length > 0) {
                return cfgPwd;
            }
        }
        // Fallback to a non-sensitive placeholder; real secret must be provided via config
        return '';
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
                // Removed hard-coded password; now obtained from configuration helper
                password: getRefreshPassword()
            })
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

    // Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
    webgoat.customjs.addBearerToken = function () {
        var headers_to_set = {};
        var accessToken = localStorage.getItem('access_token');
        if (accessToken) {
            headers_to_set.Authorization = 'Bearer ' + accessToken;
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
                Authorization: 'Bearer ' + (localStorage.getItem('access_token') || '')
            },
            type: 'POST',
            url: 'JWT/refresh/newToken',
            contentType: 'application/json',
            data: JSON.stringify({ refreshToken: refreshToken })
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

    // Expose functions that are expected to be callable from the page, if any
    window.webgoat = window.webgoat || {};
    window.webgoat.jwtRefresh = {
        login: login,
        newToken: newToken
    };
})();
