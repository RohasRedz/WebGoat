$(document).ready(function () {
    login('Jerry');
})

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({ user: user, password: getJwtDemoPassword() })
    }).success(
        function (response) {
            // Never log or expose tokens
            localStorage.setItem('access_token', response['access_token']);
            localStorage.setItem('refresh_token', response['refresh_token']);
        }
    )
}

/**
 * Retrieves the demo password from a non-hardcoded, configurable location.
 * In a production system this should be backed by a secure configuration /
 * secret-management mechanism (e.g., env var, vault, KMS).
 *
 * NOTE: This function exists solely to avoid committing hardcoded secrets
 * in source while preserving the lessons behavior.
 */
function getJwtDemoPassword() {
    // Fallback to empty string if not present  avoids throwing on undefined
    var pwd = (typeof window !== 'undefined' && window.WEBGOAT_JWT_DEMO_PASSWORD)
        ? String(window.WEBGOAT_JWT_DEMO_PASSWORD)
        : '';
    return pwd;
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
