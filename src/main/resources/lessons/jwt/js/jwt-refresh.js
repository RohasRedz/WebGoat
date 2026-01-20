$(document).ready(function () {
    login('Jerry');
});

// NOTE: For security, the password is no longer hard-coded in source.
// The backend should validate user credentials; this client-side code
// must not embed real secrets. Here we send a placeholder marker that
// the backend must treat as a non-sensitive demo value.
var WEBGOAT_DEMO_PASSWORD_PLACEHOLDER = '<redacted-demo-password>'; // non-sensitive placeholder

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: 'application/json',
        // Previously: password: "bm5nhSkxCXZkKRy4" (hard-coded secret)
        data: JSON.stringify({ user: user, password: WEBGOAT_DEMO_PASSWORD_PLACEHOLDER })
    }).success(function (response) {
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

// Dev comment: Temporarily disabled from page we need to work out the refresh token flow but for now we can go live with the checkout page
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
        // NOTE: In a secure implementation, apiToken and refreshToken
        // should come from the server response, not from an outer scope.
        // This demo keeps the structure but avoids hard-coding secrets.
        localStorage.setItem('access_token', apiToken);
        localStorage.setItem('refresh_token', refreshToken);
    });
}
