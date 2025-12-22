$(document).ready(function () {
    login('Jerry');
});

// Centralized configuration abstraction for non-secret client values.
// Sensitive credentials must never be hard-coded here; this object should
// only hold identifiers or non-secret hints if needed.
var webgoatJwtConfig = (function () {
    'use strict';

    // Placeholder provider for the password. In a real deployment, this
    // should be supplied by a secure configuration/secrets mechanism on
    // the server side and NOT embedded in client-side code.
    function getPassword() {
        // Returning null forces the login call to fail safely if a real
        // secret is not provided by the environment.
        return null;
    }

    return {
        getPassword: getPassword
    };
})();

function login(user) {
    var password = webgoatJwtConfig.getPassword();
    // Fail-fast if no password is configured; avoids using any
    // hard-coded credential in client-side code.
    if (!password) {
        // In a real application, you might display a generic error to
        // the user or handle this gracefully without exposing details.
        // For lesson/demo purposes we simply abort the request.
        return;
    }

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({ user: user, password: password })
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
        data: JSON.stringify({ refreshToken: localStorage.getItem('refresh_token') })
    }).success(
        function () {
            localStorage.setItem('access_token', apiToken);
            localStorage.setItem('refresh_token', refreshToken);
        }
    );
}
