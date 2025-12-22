$(document).ready(function () {
    login('Jerry');
})

/**
 * WARNING: For security reasons, credentials MUST NOT be hard-coded in client-side code.
 * The password is now expected to be provided via a secure runtime mechanism
 * (e.g. user input, secure server-side flow, or configuration not committed to source).
 *
 * In a real deployment this client-side password field should not exist at all for
 * authentication; instead, use a secure, server-side authentication flow.
 */
const JWT_REFRESH_PASSWORD = null; // Placeholder – must be supplied securely at runtime

function login(user) {
    if (!JWT_REFRESH_PASSWORD) {
        // Fail safe if password is not provided securely
        // In production, this should be handled by a proper UI flow rather than silent auto-login
        // Here we simply avoid making the insecure request.
        // console.warn('JWT refresh password is not configured securely; aborting login.');
        return;
    }

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({user: user, password: JWT_REFRESH_PASSWORD})
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
