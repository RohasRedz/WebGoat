$(document).ready(function () {
    login('Jerry');
});

/**
 * NOTE (Security Fix - Hardcoded Secret Removal):
 * The password used for login MUST NOT be hard-coded in source code.
 * Instead, it should be provided via a secure configuration mechanism at runtime,
 * for example:
 *   - A server-rendered meta tag
 *   - A non-guessable, non-hardcoded value injected into the page template
 *   - A secure configuration object populated from environment or secret manager
 *
 * For this automated remediation, we replace the hard-coded value with a
 * clearly named placeholder. Application owners MUST wire a secure value into
 * `webgoat.config.jwtRefreshPassword` (or equivalent) before using in production.
 */
function getJwtRefreshPassword() {
    // Placeholder ONLY. Must be set securely by server-side configuration.
    if (window.webgoat && window.webgoat.config && window.webgoat.config.jwtRefreshPassword) {
        return window.webgoat.config.jwtRefreshPassword;
    }

    // Fallback to empty string to avoid accidentally using a hardcoded secret.
    // This will likely cause authentication to fail until a proper config is provided,
    // which is safer than silently using an insecure hardcoded password.
    return "";
}

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({
            user: user,
            // Removed hard-coded password; now retrieved via a configuration hook.
            password: getJwtRefreshPassword()
        })
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
    );
}
