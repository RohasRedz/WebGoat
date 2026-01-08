$(document).ready(function () {
    // Avoid hardcoded credentials; use a non-sensitive placeholder user here.
    // Actual password/secret must be provided securely server-side, not from client code.
    login('Jerry');
});

function login(user) {
    // Retrieve password from a non-hardcoded, non-sensitive source.
    // In this educational context, we avoid embedding any real secrets in the client.
    // If a password is required for the lesson, it should be provided via a
    // server-side flow or through non-secret challenge instructions.
    var password = '';

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({ user: user, password: password })
    }).success(
        function (response) {
            // Store only the minimum required token data in localStorage.
            // Note: in production systems, consider using HttpOnly cookies instead
            // of localStorage for sensitive tokens.
            if (response && typeof response === 'object') {
                if (typeof response['access_token'] === 'string') {
                    localStorage.setItem('access_token', response['access_token']);
                }
                if (typeof response['refresh_token'] === 'string') {
                    localStorage.setItem('refresh_token', response['refresh_token']);
                }
            }
        }
    );
}

// Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs = webgoat.customjs || {};
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    var accessToken = localStorage.getItem('access_token');

    // Only attach the header if the token exists and looks like a JWT
    if (typeof accessToken === 'string' && accessToken.split('.').length === 3) {
        headers_to_set['Authorization'] = 'Bearer ' + accessToken;
    }

    return headers_to_set;
};

// Dev comment: Temporarily disabled from page we need to work out the refresh token flow
// but for now we can go live with the checkout page
function newToken() {
    var refreshToken = localStorage.getItem('refresh_token');
    var accessToken = localStorage.getItem('access_token');

    if (typeof refreshToken !== 'string' || refreshToken.length === 0) {
        // No refresh token available; do not attempt the call
        return;
    }

    $.ajax({
        headers: (function () {
            var headers = {};
            // Only send Authorization header if access token looks like a JWT
            if (typeof accessToken === 'string' && accessToken.split('.').length === 3) {
                headers['Authorization'] = 'Bearer ' + accessToken;
            }
            return headers;
        })(),
        type: 'POST',
        url: 'JWT/refresh/newToken',
        contentType: "application/json",
        data: JSON.stringify({ refreshToken: refreshToken })
    }).success(
        function (response) {
            // Expect the API to return new tokens; avoid using undeclared variables
            if (response && typeof response === 'object') {
                if (typeof response['access_token'] === 'string') {
                    localStorage.setItem('access_token', response['access_token']);
                }
                if (typeof response['refresh_token'] === 'string') {
                    localStorage.setItem('refresh_token', response['refresh_token']);
                }
            }
        }
    );
}
