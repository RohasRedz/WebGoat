$(document).ready(function () {
    login('Jerry');
})

/**
 * Retrieve the JWT password/secret from a secure configuration source.
 * In production, this should come from a server-side configuration or
 * a securely injected value rather than being hard-coded in client code.
 */
function getJwtPassword() {
    // Placeholder for configuration-driven secret; do NOT hard-code real secrets here.
    // Example: this value should be injected at build time or retrieved via a secure API.
    return window.WEBGOAT_JWT_PASSWORD || '';
}

function login(user) {
    const password = getJwtPassword();
    if (!password) {
        // Fail-safe: avoid sending login request with an empty or missing password.
        // In a real deployment, this should be handled through a secure flow that
        // never exposes secrets in front-end code.
        console.warn('JWT password is not configured; login request will not be sent.');
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
    )
}

// Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    headers_to_set['Authorization'] = 'Bearer ' + localStorage.getItem('access_token');
    return headers_to_set;
}

// Dev comment: Temporarily disabled from page; the refresh token flow must be implemented securely.
// Ensure that tokens are handled only on the server side and are not exposed unnecessarily.
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
    )
}
