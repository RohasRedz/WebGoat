$(document).ready(function () {
    login('Jerry');
});

/**
 * Derive the password from a configurable source instead of hardcoding it in code.
 * In a real deployment this should be provided via a secure configuration channel
 * (e.g., environment variable injected at build time or server-rendered meta tag)
 * rather than being a literal string in source.
 */
function getConfiguredPassword() {
    // Example: read from a meta tag if present (server-controlled), otherwise fall back
    // to a non-secret placeholder that must be overridden in production.
    var meta = document.querySelector('meta[name="jwt-refresh-password"]');
    if (meta && meta.content) {
        return meta.content;
    }

    // NOTE: This fallback value is intentionally non-sensitive and MUST be overridden
    // in any real deployment via a secure configuration mechanism.
    return 'CHANGE_ME_IN_SECURE_CONFIG';
}

function login(user) {
    var password = getConfiguredPassword();

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({user: user, password: password})
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
