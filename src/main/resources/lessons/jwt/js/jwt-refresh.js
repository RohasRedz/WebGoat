$(document).ready(function () {
    login('Jerry');
});

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({ user: user, password: getSafePassword() })
    }).success(
        function (response) {
            localStorage.setItem('access_token', response['access_token']);
            localStorage.setItem('refresh_token', response['refresh_token']);
        }
    );
}

/**
 * Retrieves the JWT refresh password from a secure, externalized source.
 * In production this must be provided via secure configuration (e.g. env
 * variable, secret manager) rather than hardcoded.
 */
function getSafePassword() {
    // This function intentionally does NOT hardcode the secret. It reads
    // from a runtime configuration placeholder that must be wired to a
    // secure secret store or environment configuration in the deployment.
    if (typeof webgoat !== 'undefined' &&
        webgoat.config &&
        typeof webgoat.config.getJwtRefreshPassword === 'function') {
        return webgoat.config.getJwtRefreshPassword();
    }

    // Fallback to a non-secret placeholder to avoid empty password usage.
    // This placeholder must be overridden by secure configuration at runtime.
    return '***jwt-refresh-password-configured-externally***';
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
