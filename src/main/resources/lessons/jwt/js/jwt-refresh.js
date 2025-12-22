$(document).ready(function () {
    // Use a non-hardcoded password sourced from a configuration object to avoid embedding secrets in code.
    login('Jerry');
});

(function () {
    'use strict';

    /**
     * Application configuration for JWT demo.
     * In a real deployment this should be injected from a secure configuration source
     * (e.g., environment variable, server-side rendered config, or secrets manager),
     * not hardcoded in client-side JavaScript.
     */
    var webgoatJwtConfig = window.webgoatJwtConfig || {};

    /**
     * Derive the password from a configurable source instead of hardcoding it.
     * Fallback value is intentionally non-sensitive demo text to avoid real secret exposure.
     */
    function getJwtDemoPassword() {
        if (typeof webgoatJwtConfig.jwtDemoPassword === 'string' &&
            webgoatJwtConfig.jwtDemoPassword.length > 0) {
            return webgoatJwtConfig.jwtDemoPassword;
        }
        // Fallback to a non-secret placeholder suitable for demo environments only.
        return 'demo-password-not-for-production';
    }

    function login(user) {
        $.ajax({
            type: 'POST',
            url: 'JWT/refresh/login',
            contentType: "application/json",
            data: JSON.stringify({
                user: user,
                // Use configurable password instead of a hard-coded literal
                password: getJwtDemoPassword()
            })
        }).success(
            function (response) {
                // Store tokens as before; do not log or expose them.
                localStorage.setItem('access_token', response['access_token']);
                localStorage.setItem('refresh_token', response['refresh_token']);
            }
        );
    }

    // Expose login only if needed by other scripts
    window.login = login;

    // Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
    window.webgoat = window.webgoat || {};
    window.webgoat.customjs = window.webgoat.customjs || {};
    window.webgoat.customjs.addBearerToken = function () {
        var headers_to_set = {};
        headers_to_set['Authorization'] = 'Bearer ' + localStorage.getItem('access_token');
        return headers_to_set;
    };

    // Dev comment: Temporarily disabled from page we need to work out the refresh token flow
    // but for now we can go live with the checkout page
    function newToken() {
        // retain behavior: use refresh token from localStorage
        var refreshTokenValue = localStorage.getItem('refresh_token');
        $.ajax({
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('access_token')
            },
            type: 'POST',
            url: 'JWT/refresh/newToken',
            data: JSON.stringify({ refreshToken: refreshTokenValue })
        }).success(
            function (response) {
                // Assume API returns new tokens; do not reference undefined variables.
                if (response && response.access_token && response.refresh_token) {
                    localStorage.setItem('access_token', response.access_token);
                    localStorage.setItem('refresh_token', response.refresh_token);
                }
            }
        );
    }

    // Expose newToken if other scripts rely on it.
    window.newToken = newToken;
})();
