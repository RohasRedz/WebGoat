$(document).ready(function () {
    login('Jerry');
});

/**
 * Retrieve the JWT password/secret from a runtime configuration source.
 * In a browser environment this should NOT be hard-coded in source;
 * instead, it should be provided via a secure configuration mechanism
 * (e.g., server-side templated config, meta tag, or other non-public channel).
 */
function getJwtPassword() {
    // Expect a non-hard-coded value to be injected by the server or build system.
    // Fallback is intentionally empty to avoid introducing a hard-coded secret.
    var passwordFromConfig = window.WEBGOAT_JWT_PASSWORD || '';
    return passwordFromConfig;
}

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: 'application/json',
        data: JSON.stringify({
            user: user,
            // Use configured password/secret instead of hard-coded literal
            password: getJwtPassword()
        })
    }).success(function (response) {
        localStorage.setItem('access_token', response['access_token']);
        localStorage.setItem('refresh_token', response['refresh_token']);
    });
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
    }).success(function () {
        localStorage.setItem('access_token', apiToken);
        localStorage.setItem('refresh_token', refreshToken);
    });
}
