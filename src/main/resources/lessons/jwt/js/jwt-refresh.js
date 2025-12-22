$(document).ready(function () {
    // Use a non-sensitive, non-secret placeholder value or require user/password from a secure flow.
    // For this training/demo flow we keep the hard-coded username but remove hard-coded secrets.
    login('Jerry');
});

function login(user) {
    // Retrieve the password or token from a secure configuration source if available.
    // In this training environment, we avoid hard-coding any real secret and fall back to a
    // non-sensitive placeholder that does NOT represent a production credential.
    var password = window.webgoat && window.webgoat.config && window.webgoat.config.jwtDemoPassword
        ? String(window.webgoat.config.jwtDemoPassword)
        : 'demo-password-not-for-production';

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({ user: user, password: password })
    }).success(function (response) {
        // Store only what is strictly required, and avoid logging or exposing tokens.
        if (response && typeof response.access_token === 'string') {
            localStorage.setItem('access_token', response['access_token']);
        }
        if (response && typeof response.refresh_token === 'string') {
            localStorage.setItem('refresh_token', response['refresh_token']);
        }
    });
}

//Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    var accessToken = localStorage.getItem('access_token');

    // Do not log tokens or expose them; just attach when present.
    if (typeof accessToken === 'string' && accessToken.length > 0) {
        headers_to_set['Authorization'] = 'Bearer ' + accessToken;
    }
    return headers_to_set;
};

//Dev comment: Temporarily disabled from page we need to work out the refresh token flow but for now we can go live with the checkout page
function newToken() {
    var refreshToken = localStorage.getItem('refresh_token');
    if (!refreshToken) {
        return;
    }

    $.ajax({
        headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('access_token')
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        contentType: "application/json",
        data: JSON.stringify({ refreshToken: refreshToken })
    }).success(function (response) {
        // Use the tokens returned by the backend instead of undefined variables
        if (response && typeof response.access_token === 'string') {
            localStorage.setItem('access_token', response.access_token);
        }
        if (response && typeof response.refresh_token === 'string') {
            localStorage.setItem('refresh_token', response.refresh_token);
        }
    });
}
