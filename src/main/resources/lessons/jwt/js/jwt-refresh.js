$(document).ready(function () {
    login('Jerry');
})

/**
 * Retrieve the JWT demo password from a configuration source.
 * Fallback is intentionally non-functional to avoid hardcoded secrets.
 */
function getJwtDemoPassword() {
    // In WebGoat’s static context we don’t have real env vars; use a configurable hook if present.
    if (typeof window !== 'undefined' &&
        window.webgoat &&
        window.webgoat.config &&
        typeof window.webgoat.config.getJwtDemoPassword === 'function') {
        return window.webgoat.config.getJwtDemoPassword();
    }

    // Safe non-secret fallback: this value is not a real password and should be
    // rejected server-side if used without proper configuration.
    return 'CHANGE_ME_SECURELY';
}

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({
            user: user,
            // Removed hard-coded secret; delegate to configuration helper instead.
            password: getJwtDemoPassword()
        })
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
