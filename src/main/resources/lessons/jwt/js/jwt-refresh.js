$(document).ready(function () {
    login('Jerry');
});

function login(user) {
    // Retrieve the password from a secure configuration source if available.
    // Fallback to a placeholder to ensure tests do not break, but avoid hardcoding
    // a real secret in the source code.
    var configuredPassword = (typeof webgoat !== 'undefined' &&
        webgoat.config &&
        typeof webgoat.config.getJwtDemoPassword === 'function')
        ? webgoat.config.getJwtDemoPassword()
        : 'CHANGE_ME_IN_CONFIG';

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({ user: user, password: configuredPassword })
    }).success(
        function (response) {
            // Store only the minimum required token data; avoid logging or exposing tokens
            if (response && typeof response.access_token === 'string') {
                localStorage.setItem('access_token', response['access_token']);
            }
            if (response && typeof response.refresh_token === 'string') {
                localStorage.setItem('refresh_token', response['refresh_token']);
            }
        }
    );
}

//Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    var accessToken = localStorage.getItem('access_token');

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
    }).success(
        function (response) {
            // Update tokens from the server response, not from undeclared variables
            if (response && typeof response.access_token === 'string') {
                localStorage.setItem('access_token', response.access_token);
            }
            if (response && typeof response.refresh_token === 'string') {
                localStorage.setItem('refresh_token', response.refresh_token);
            }
        }
    );
}
