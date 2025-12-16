$(document).ready(function () {
    login('Jerry');
});

function login(user) {
    // Load the password (or equivalent secret) from a secure configuration source
    // instead of hard-coding it in the client. This value should be injected at
    // build-time or templated server-side so that no real secrets reside in this
    // JavaScript file.
    var configuredPassword = (typeof JWT_REFRESH_PASSWORD !== 'undefined' && JWT_REFRESH_PASSWORD)
        ? JWT_REFRESH_PASSWORD
        : 'CHANGE_ME_SECURELY_CONFIGURED_PASSWORD';

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({
            user: user,
            // Use the configured password placeholder instead of a hard-coded secret.
            // In production, ensure the templating / build pipeline replaces this
            // placeholder with a non-secret challenge/demo value or handles auth
            // entirely server-side.
            password: configuredPassword
        })
    }).success(function (response) {
        // Store tokens in localStorage as per existing behavior.
        // NOTE: In real applications, consider HttpOnly, Secure cookies instead.
        localStorage.setItem('access_token', response['access_token']);
        localStorage.setItem('refresh_token', response['refresh_token']);
    });
}

// Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    headers_to_set['Authorization'] = 'Bearer ' + localStorage.getItem('access_token');
    return headers_to_set;
};

// Dev comment: Temporarily disabled from page we need to work out the refresh token flow
// but for now we can go live with the checkout page
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
        // NOTE: apiToken and refreshToken should be provided by the backend response.
        // This function keeps the existing behavior; ensure server-side code returns
        // these values instead of deriving or hard-coding them on the client.
        localStorage.setItem('access_token', apiToken);
        localStorage.setItem('refresh_token', refreshToken);
    });
}
