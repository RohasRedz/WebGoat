$(document).ready(function () {
    login('Jerry');
});

function login(user) {
    // Retrieve password/secret from a secure runtime source instead of hard-coding it.
    // NOTE: In a browser context, truly secret values should NOT be shipped to the client.
    // Here we avoid embedding the actual secret and instead expect the backend to handle
    // authentication based solely on non-secret user data, or to inject non-sensitive
    // configuration via a safe mechanism if strictly required.
    var password = webgoat && webgoat.config && webgoat.config.jwtDemoPassword
        ? webgoat.config.jwtDemoPassword
        : '***redacted***';

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        // Do NOT embed real secrets in client-side JS. This placeholder indicates that
        // the real secret must be supplied securely at runtime (e.g., server-side).
        data: JSON.stringify({ user: user, password: password })
    }).success(
        function (response) {
            // Store only what is strictly required; avoid logging or exposing tokens.
            localStorage.setItem('access_token', response['access_token']);
            localStorage.setItem('refresh_token', response['refresh_token']);
        }
    );
}

// Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    var accessToken = localStorage.getItem('access_token');

    // Avoid sending empty/undefined tokens.
    if (typeof accessToken === 'string' && accessToken.length > 0) {
        headers_to_set['Authorization'] = 'Bearer ' + accessToken;
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
            'Authorization': 'Bearer ' + localStorage.getItem('access_token')
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        contentType: "application/json",
        data: JSON.stringify({ refreshToken: refreshToken })
    }).success(
        function (response) {
            // Use values returned from the server; do not rely on undeclared globals.
            if (response && response.access_token) {
                localStorage.setItem('access_token', response.access_token);
            }
            if (response && response.refresh_token) {
                localStorage.setItem('refresh_token', response.refresh_token);
            }
        }
    );
}
