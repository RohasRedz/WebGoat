(function () {
    'use strict';

    /**
     * Retrieve the JWT demo password from a non-hardcoded source.
     * In this WebGoat context, we avoid committing real secrets and use
     * a placeholder that can be replaced by secure configuration at runtime.
     */
    function getJwtDemoPassword() {
        // Prefer an environment/config-driven value when available.
        // Fallback to a non-sensitive placeholder that does NOT represent a real credential.
        if (typeof window !== 'undefined' &&
            window.webgoat &&
            window.webgoat.config &&
            typeof window.webgoat.config.jwtDemoPassword === 'string' &&
            window.webgoat.config.jwtDemoPassword.length > 0) {
            return window.webgoat.config.jwtDemoPassword;
        }

        // Non-sensitive placeholder; should be overridden by configuration in real deployments.
        return 'CHANGE_ME_JWT_DEMO_PASSWORD';
    }

    $(document).ready(function () {
        login('Jerry');
    });

    function login(user) {
        $.ajax({
            type: 'POST',
            url: 'JWT/refresh/login',
            contentType: "application/json",
            data: JSON.stringify({
                user: user,
                // Removed hard-coded secret; now retrieved from configuration helper
                password: getJwtDemoPassword()
            })
        }).success(
            function (response) {
                localStorage.setItem('access_token', response['access_token']);
                localStorage.setItem('refresh_token', response['refresh_token']);
            }
        );
    }

    //Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
    webgoat.customjs.addBearerToken = function () {
        var headers_to_set = {};
        headers_to_set['Authorization'] = 'Bearer ' + localStorage.getItem('access_token');
        return headers_to_set;
    };

    //Dev comment: Temporarily disabled from page we need to work out the refresh token flow but for now we can go live with the checkout page
    function newToken() {
        localStorage.getItem('refreshToken');
        $.ajax({
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('access_token')
            },
            type: 'POST',
            url: 'JWT/refresh/newToken',
            data: JSON.stringify({refreshToken: localStorage.getItem('refresh_token')})
        }).success(
            function () {
                localStorage.setItem('access_token', apiToken);
                localStorage.setItem('refresh_token', refreshToken);
            }
        );
    }
})();
