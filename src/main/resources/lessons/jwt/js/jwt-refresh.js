$(document).ready(function () {
    // Require explicit user interaction and password entry instead of auto-login with hard-coded password
    // Example: bind login to a button click that reads user and password from input fields.
    $('#jwt-login-button').on('click', function () {
        var user = $('#jwt-username').val();
        var password = $('#jwt-password').val();

        if (typeof user === 'string' && typeof password === 'string' && user.length > 0 && password.length > 0) {
            login(user, password);
        }
    });
});

function login(user, password) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({ user: user, password: password })
    }).success(
        function (response) {
            // Store tokens locally but never log or expose them
            if (response && typeof response.access_token === 'string') {
                localStorage.setItem('access_token', response['access_token']);
            }
            if (response && typeof response.refresh_token === 'string') {
                localStorage.setItem('refresh_token', response['refresh_token']);
            }
        }
    );
}

// Dev note: Pass token as header as we had an issue with tokens ending up in the access_log
// Ensure these tokens are never logged or exposed elsewhere.
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    var accessToken = localStorage.getItem('access_token');
    if (typeof accessToken === 'string' && accessToken.length > 0) {
        headers_to_set['Authorization'] = 'Bearer ' + accessToken;
    }
    return headers_to_set;
}

// Dev note: Temporarily disabled from page; ensure refresh tokens are handled securely
// and never logged or exposed. This function keeps behavior but avoids referencing
// undefined variables and unsafe token handling.
function newToken() {
    var refreshToken = localStorage.getItem('refresh_token');
    var accessToken = localStorage.getItem('access_token');

    if (!refreshToken || !accessToken) {
        return;
    }

    $.ajax({
        headers: {
            'Authorization': 'Bearer ' + accessToken
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        contentType: "application/json",
        data: JSON.stringify({ refreshToken: refreshToken })
    }).success(
        function (response) {
            if (response && typeof response.access_token === 'string') {
                localStorage.setItem('access_token', response['access_token']);
            }
            if (response && typeof response.refresh_token === 'string') {
                localStorage.setItem('refresh_token', response['refresh_token']);
            }
        }
    );
}
