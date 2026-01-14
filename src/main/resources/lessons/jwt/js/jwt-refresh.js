// NOTE: Avoid hard-coded secrets. Use configuration or server-provided values instead.
(function () {
    'use strict';

    // In this client-side context we cannot safely source secrets.
    // Use a clearly non-sensitive placeholder and rely on the backend to enforce real authentication.
    var NON_SENSITIVE_PLACEHOLDER_PASSWORD = '***REDACTED***';

    $(document).ready(function () {
        login('Jerry');
    });

    function login(user) {
        $.ajax({
            type: 'POST',
            url: 'JWT/refresh/login',
            contentType: "application/json",
            // Removed hard-coded real-looking password; replaced with a non-sensitive placeholder
            data: JSON.stringify({user: user, password: NON_SENSITIVE_PLACEHOLDER_PASSWORD})
        }).success(
            function (response) {
                // Only store tokens that must be used client-side; avoid logging them.
                if (response && typeof response.access_token === 'string') {
                    localStorage.setItem('access_token', response['access_token']);
                }
                if (response && typeof response.refresh_token === 'string') {
                    localStorage.setItem('refresh_token', response['refresh_token']);
                }
            }
        );
    }

    // Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
    // Ensure we never log tokens and only expose them in Authorization headers.
    webgoat.customjs.addBearerToken = function () {
        var headers_to_set = {};
        var accessToken = localStorage.getItem('access_token');
        if (typeof accessToken === 'string' && accessToken.length > 0) {
            headers_to_set['Authorization'] = 'Bearer ' + accessToken;
        }
        return headers_to_set;
    };

    // Dev comment: Temporarily disabled from page we need to work out the refresh token flow
    // but for now we can go live with the checkout page
    function newToken() {
        var refreshToken = localStorage.getItem('refresh_token');
        $.ajax({
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('access_token')
            },
            type: 'POST',
            url: 'JWT/refresh/newToken',
            data: JSON.stringify({refreshToken: refreshToken})
        }).success(
            function (response) {
                // Use values from the response instead of undeclared globals to avoid leaking/stale tokens
                if (response && typeof response.access_token === 'string') {
                    localStorage.setItem('access_token', response.access_token);
                }
                if (response && typeof response.refresh_token === 'string') {
                    localStorage.setItem('refresh_token', response.refresh_token);
                }
            }
        );
    }
})();
