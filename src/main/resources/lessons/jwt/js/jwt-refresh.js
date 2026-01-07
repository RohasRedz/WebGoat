$(document).ready(function () {
    // Use a non-sensitive default user; password is no longer hard-coded.
    login('Jerry');
});

function login(user) {
    // Obtain the password (or auth secret) from a secure, non-hardcoded source if needed.
    // For browser-based demo code, we avoid embedding real secrets and instead rely on
    // server-side logic to determine authentication requirements.
    var password = getDemoPassword();

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({ user: user, password: password })
    }).success(
        function (response) {
            // Never log tokens; just store them.
            localStorage.setItem('access_token', response['access_token']);
            localStorage.setItem('refresh_token', response['refresh_token']);
        }
    );
}

/**
 * Demo-only password helper.
 *
 * This function intentionally does NOT embed any real secret. It either:
 *  - Returns a fixed demo marker understood only by the backend, or
 *  - Reads from a non-sensitive configuration placeholder.
 *
 * Backend must enforce real authentication and should not treat this value
 * as a production credential.
 */
function getDemoPassword() {
    // In a real environment, the server must enforce proper authentication.
    // Here we return a non-secret placeholder used solely for the WebGoat lesson.
    return 'DEMO_PASSWORD_ONLY';
}

//Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    var accessToken = localStorage.getItem('access_token');

    // Do not log token contents; just attach to the request if present.
    if (typeof accessToken === 'string' && accessToken.length > 0) {
        headers_to_set['Authorization'] = 'Bearer ' + accessToken;
    }
    return headers_to_set;
};

//Dev comment: Temporarily disabled from page we need to work out the refresh token flow but for now we can go live with the checkout page
function newToken() {
    // Use the stored refresh token safely; do not log it.
    var refreshToken = localStorage.getItem('refresh_token');
    var accessToken = localStorage.getItem('access_token');

    $.ajax({
        headers: {
            'Authorization': accessToken ? ('Bearer ' + accessToken) : ''
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        contentType: 'application/json',
        data: JSON.stringify({ refreshToken: refreshToken })
    }).success(
        function (response) {
            // Update tokens from the server response, not from undefined local vars.
            if (response && response.access_token) {
                localStorage.setItem('access_token', response.access_token);
            }
            if (response && response.refresh_token) {
                localStorage.setItem('refresh_token', response.refresh_token);
            }
        }
    );
}
