$(document).ready(function () {
    login('Jerry');
})

function login(user) {
    // Avoid hard-coded secrets: obtain password from a configurable, non-committed source if available.
    // If not configured, do NOT fall back to a static default; instead, fail closed.
    var password = null;

    if (window && window.WEBGOAT_CONFIG && typeof window.WEBGOAT_CONFIG.jwtPassword === 'string') {
        password = window.WEBGOAT_CONFIG.jwtPassword;
    }

    if (!password) {
        // Fail closed: do not attempt login with a hard-coded or empty password.
        // This log message is intentionally generic and does not expose any secret material.
        /* eslint-disable no-console */
        console.warn('JWT refresh login password is not configured; aborting login request to prevent use of hard-coded credentials.');
        /* eslint-enable no-console */
        return;
    }

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({ user: user, password: password })
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
        data: JSON.stringify({ refreshToken: localStorage.getItem('refresh_token') })
    }).success(
        function () {
            localStorage.setItem('access_token', apiToken);
            localStorage.setItem('refresh_token', refreshToken);
        }
    )
}
