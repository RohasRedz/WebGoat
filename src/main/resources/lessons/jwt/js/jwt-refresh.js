(function () {
    'use strict';

    // NOTE:
    // The original code contained a hard-coded password literal in the client.
    // This has been removed to avoid exposing secrets in front-end code.
    // The backend must be configured to handle secure, user-provided credentials
    // or a non-secret demo password that is not relied on for real security.

    // Configuration for the demo user. No secrets are stored here.
    var DEMO_USER = 'Jerry';

    /**
     * Obtain a password for the given user.
     *
     * For real applications, the password must come from user input (e.g., a form)
     * and travel over HTTPS only. Hard-coded secrets must never be used.
     * Here it returns an empty string by default to avoid embedding secrets
     * and to force backend-side handling for actual authentication.
     *
     * @param {string} user
     * @returns {string}
     */
    function getPasswordForUser(user) {
        // Placeholder non-secret value; backend must not rely on this as a real secret.
        // If needed for the lesson flow, the backend can treat this as a non-sensitive
        // demo/password or bypass real authentication for this particular training scenario.
        if (typeof user === 'string' && user.length > 0) {
            return '';
        }
        return '';
    }

    $(document).ready(function () {
        login(DEMO_USER);
    });

    function login(user) {
        var password = getPasswordForUser(user);

        $.ajax({
            type: 'POST',
            url: 'JWT/refresh/login',
            contentType: 'application/json',
            data: JSON.stringify({
                user: user,
                // The original hard-coded secret has been removed to prevent
                // leaking credentials in client-side code.
                password: password
            })
        }).success(function (response) {
            if (response && typeof response === 'object') {
                if (typeof response.access_token === 'string') {
                    localStorage.setItem('access_token', response.access_token);
                }
                if (typeof response.refresh_token === 'string') {
                    localStorage.setItem('refresh_token', response.refresh_token);
                }
            }
        });
    }

    // Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
    // NOTE: Access tokens remain in memory (localStorage) as per the original design;
    // for production systems, prefer HttpOnly cookies and short-lived tokens.
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
        // Keep behavior: read refresh token from storage and request a new token using Authorization header
        var accessToken = localStorage.getItem('access_token');
        var refreshToken = localStorage.getItem('refresh_token');

        $.ajax({
            headers: {
                'Authorization': 'Bearer ' + (accessToken || '')
            },
            type: 'POST',
            url: 'JWT/refresh/newToken',
            contentType: 'application/json',
            data: JSON.stringify({
                refreshToken: refreshToken || ''
            })
        }).success(function (response) {
            // Use response from server rather than undefined variables.
            if (response && typeof response === 'object') {
                if (typeof response.access_token === 'string') {
                    localStorage.setItem('access_token', response.access_token);
                }
                if (typeof response.refresh_token === 'string') {
                    localStorage.setItem('refresh_token', response.refresh_token);
                }
            }
        });
    }

    // Expose newToken if the original lesson/demo relies on it via the global object
    webgoat.customjs = webgoat.customjs || {};
    webgoat.customjs.newToken = newToken;
})();
