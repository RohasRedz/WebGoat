$(document).ready(function () {
    // NOTE: The password is no longer hard-coded.
    // It must be provided securely at runtime (e.g., from server-rendered config or user input).
    var securePassword = getJwtLoginPassword();
    if (securePassword) {
        login('Jerry', securePassword);
    } else {
        // In a real deployment, handle missing password appropriately (e.g., show error UI).
        // Avoid logging sensitive values; only log that configuration is missing.
        if (window.console && console.warn) {
            console.warn('JWT login password is not configured; login request not sent.');
        }
    }
});

/**
 * Retrieve the JWT login password from a secure, non-hard-coded source.
 *
 * Expected patterns (examples, depending on app setup):
 * - A server-rendered, non-sensitive placeholder that points to a secure store.
 * - A value injected into the page via a secure mechanism (NOT hard-coded secret in JS).
 * - A password entered by the user via a form, then passed into login().
 *
 * This function intentionally avoids embedding any real secret.
 */
function getJwtLoginPassword() {
    // Example pattern 1: Server could render a non-secret config object like:
    // window.webgoatConfig = { jwtLoginPassword: '***' }; // placeholder, not an actual secret
    // and the real password is resolved server-side or via a secure channel.
    if (window.webgoatConfig && typeof window.webgoatConfig.jwtLoginPassword === 'string') {
        return window.webgoatConfig.jwtLoginPassword;
    }

    // Example pattern 2: If a user enters a password in a form field (preferred for exercises),
    // you could read it here (ensure HTTPS and secure handling):
    // var field = document.getElementById('jwt-login-password');
    // return field ? field.value : null;

    // Default: no password available
    return null;
}

function login(user, password) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        // Do NOT log this body or password; it's sensitive.
        data: JSON.stringify({ user: user, password: password })
    }).success(
        function (response) {
            localStorage.setItem('access_token', response['access_token']);
            localStorage.setItem('refresh_token', response['refresh_token']);
        }
    )
}

//Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    headers_to_set['Authorization'] = 'Bearer ' + localStorage.getItem('access_token');
    return headers_to_set;
}

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
            // NOTE: apiToken and refreshToken should be provided by the server response
            // and must not be hard-coded or derived insecurely on the client.
            localStorage.setItem('access_token', apiToken);
            localStorage.setItem('refresh_token', refreshToken);
        }
    )
}
