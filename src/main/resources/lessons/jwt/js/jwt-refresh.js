$(document).ready(function () {
    login('Jerry');
});

function login(user) {
    // Retrieve password from a secure, externalized configuration instead of hard-coding it
    // Expectation: webgoat.config.jwtPassword is injected at runtime via secure configuration
    var password = (window.webgoat &&
        window.webgoat.config &&
        typeof window.webgoat.config.jwtPassword === 'string')
        ? window.webgoat.config.jwtPassword
        : null;

    if (!password) {
        // Fail closed if no secure password is configured
        // In a real deployment, this condition should be handled gracefully by the backend/UI.
        // We avoid logging the actual password or other sensitive details.
        console.error('JWT login password is not configured securely.');
        return;
    }

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: 'application/json',
        data: JSON.stringify({ user: user, password: password })
    }).success(
        function (response) {
            localStorage.setItem('access_token', response['access_token']);
            localStorage.setItem('refresh_token', response['refresh_token']);
        }
    );
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
    }).success(
        function () {
            localStorage.setItem('access_token', apiToken);
            localStorage.setItem('refresh_token', refreshToken);
        }
    );
}
