$(document).ready(function () {
    login('Jerry');
})

/**
 * Retrieve the password/secret used for JWT refresh login from a non-hardcoded source.
 * NOTE: In this demo code we DO NOT embed the real secret in the frontend.
 * In production, this value must come from a secure server-side configuration
 * or a token issued by the backend  never from hardcoded client-side JS.
 */
function getRefreshLoginPassword() {
    // TODO: Wire this to a secure, server-provided value (e.g., via a dedicated endpoint or config bootstrap),
    //       ensuring the actual secret is never exposed in client-side code.
    // For now, return null to avoid shipping a hardcoded password in the source bundle.
    return null;
}

function login(user) {
    const password = getRefreshLoginPassword();

    // If no password is configured, fail fast without sending a bogus hardcoded credential.
    if (!password) {
        // In a real app, you might handle this more gracefully in the UI.
        // Here we simply avoid making a request with a hardcoded or missing secret.
        // console.warn('JWT refresh password not configured; skipping login request.');
        return;
    }

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({user: user, password: password})
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
        data: JSON.stringify({refreshToken: localStorage.getItem('refresh_token')})
    }).success(
        function () {
            localStorage.setItem('access_token', apiToken);
            localStorage.setItem('refresh_token', refreshToken);
        }
    )
}
