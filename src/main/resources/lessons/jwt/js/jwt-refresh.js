$(document).ready(function () {
    // For demo purposes we call login with a user only; the secret/password is no longer hard-coded here.
    // In a real deployment, credentials should be supplied securely (e.g., via user input over HTTPS).
    login('Jerry');
});

/**
 * Perform login.
 *
 * NOTE:
 * - The password/secret is intentionally not hard-coded here to avoid embedding secrets in client code.
 * - For this demo, password is an optional parameter; if omitted, callers must obtain it securely
 *   (e.g., via user input) instead of hardcoding.
 *
 * @param {string} user - Username to authenticate.
 * @param {string} [password] - Optional password; when not provided, no password field is sent in the body.
 */
function login(user, password) {
    var payload = { user: user };

    // Only include password if explicitly provided; prevents hard-coded credentials in the source.
    if (typeof password === 'string' && password.length > 0) {
        payload.password = password;
    }

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify(payload)
    }).success(
        function (response) {
            // Store tokens as before; consider secure storage and HTTPS in production.
            localStorage.setItem('access_token', response['access_token']);
            localStorage.setItem('refresh_token', response['refresh_token']);
        }
    );
}

// Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    headers_to_set['Authorization'] = 'Bearer ' + localStorage.getItem('access_token');
    return headers_to_set;
};

// Dev comment: Temporarily disabled from page we need to work out the refresh token flow but for now we can go live with the checkout page
function newToken() {
    // Note: previously unused localStorage.getItem('refreshToken'); left out as it had no effect.
    $.ajax({
        headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('access_token')
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        data: JSON.stringify({ refreshToken: localStorage.getItem('refresh_token') })
    }).success(
        function (response) {
            // Use values returned by the backend instead of undeclared variables apiToken/refreshToken.
            if (response && response.access_token && response.refresh_token) {
                localStorage.setItem('access_token', response.access_token);
                localStorage.setItem('refresh_token', response.refresh_token);
            }
        }
    );
}
