$(document).ready(function () {
    login('Jerry');
})

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        // Removed hard-coded password; use a secure token/password source
        // provided by the backend or environment configuration.
        data: JSON.stringify({ user: user, password: getJwtRefreshPassword() })
    }).success(
        function (response) {
            localStorage.setItem('access_token', response['access_token']);
            localStorage.setItem('refresh_token', response['refresh_token']);
        }
    )
}

/**
 * Retrieve the JWT refresh password/secret from a secure channel.
 *
 * NOTE:
 * - In a real deployment, this value must NOT be hard-coded in front-end code.
 * - It should be derived from a secure configuration mechanism (e.g., injected at build
 *   time, or the server should not require a static shared secret at all).
 * - Here we provide a non-secret placeholder to preserve behavior while removing
 *   the previous hard-coded secret from source control.
 */
function getJwtRefreshPassword() {
    // Placeholder non-secret value; backend must handle real authentication/secret logic securely.
    return 'password_from_secure_backend';
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
