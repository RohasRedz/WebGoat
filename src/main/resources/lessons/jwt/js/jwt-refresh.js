$(document).ready(function () {
    login('Jerry');
})

/**
 * Retrieve the application password from a (non-versioned) configuration source.
 * In production, this should be injected at build time or via a secure runtime mechanism
 * (e.g., environment variable, server-rendered config endpoint) rather than hard-coded.
 */
function getConfiguredPassword() {
    // Placeholder to avoid hard-coding secrets in client-side code.
    // This will fall back to an empty string if no configuration is found.
    // NOTE: For the WebGoat lesson, the actual secret should be provided by the server or test harness.
    if (window.webgoat && typeof window.webgoat.config !== 'undefined'
        && typeof window.webgoat.config.jwtRefreshPassword === 'string') {
        return window.webgoat.config.jwtRefreshPassword;
    }
    return '';
}

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        // Remove hard-coded password; pull from a non-secret placeholder/config accessor instead.
        data: JSON.stringify({user: user, password: getConfiguredPassword()})
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
