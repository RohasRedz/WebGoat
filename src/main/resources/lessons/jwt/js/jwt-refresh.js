(function () {
    'use strict';

    // Configuration: keep sensitive values out of source; use placeholder and
    // retrieve real credential via a secure channel (e.g., injected at build time)
    // IMPORTANT: In this training code, we avoid embedding the real password.
    // The backend should be updated to no longer require a static password,
    // or to derive it from a secure configuration source.
    function getJwtDemoPassword() {
        // Return a non-sensitive placeholder. The server-side lesson logic
        // should be adapted accordingly so that no real secret is required
        // from the client.
        return 'DEMO_ONLY_NO_SECRET';
    }

    function login(user) {
        $.ajax({
            type: 'POST',
            url: 'JWT/refresh/login',
            contentType: 'application/json',
            data: JSON.stringify({ user: user, password: getJwtDemoPassword() })
        }).done(function (response) {
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

    //Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
    webgoat.customjs.addBearerToken = function () {
        var headers_to_set = {};
        var accessToken = localStorage.getItem('access_token');
        if (typeof accessToken === 'string' && accessToken.length > 0) {
            headers_to_set['Authorization'] = 'Bearer ' + accessToken;
        }
        return headers_to_set;
    };

    //Dev comment: Temporarily disabled from page we need to work out the refresh token flow but for now we can go live with the checkout page
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
            contentType: 'application/json',
            data: JSON.stringify({ refreshToken: refreshToken })
        }).done(function (response) {
            // Expecting the backend to return new tokens explicitly
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

    // Automatically trigger login for demo user without exposing a real password
    $(document).ready(function () {
        login('Jerry');
    });

    // Expose newToken if other scripts need to invoke it
    window.webgoat = window.webgoat || {};
    window.webgoat.jwtRefresh = window.webgoat.jwtRefresh || {};
    window.webgoat.jwtRefresh.newToken = newToken;
})();
