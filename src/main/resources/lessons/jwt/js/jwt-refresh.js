$(document).ready(function () {
    login('Jerry');
});

var webgoat = window.webgoat || {};
webgoat.customjs = webgoat.customjs || {};

/**
 * Retrieve the password from a safer runtime source rather than keeping it hardcoded in the script.
 * In a real deployment this should come from a secure backend mechanism, not from frontend code.
 */
function getLoginPassword() {
    // Prefer a nonliteral, runtime source. Fallback only if absolutely necessary.
    // For compatibility with existing deployment tooling, allow overriding via a data attribute or a
    // preinjected global variable. This avoids committing the secret directly in the source code.
    try {
        var metaPasswordElement = document.querySelector('meta[name="webgoat-jwt-password"]');
        if (metaPasswordElement && metaPasswordElement.content) {
            return metaPasswordElement.content;
        }
    } catch (e) {
        // Silent catch  do not leak details.
    }

    if (window.WEBGOAT_JWT_PASSWORD && typeof window.WEBGOAT_JWT_PASSWORD === 'string') {
        return window.WEBGOAT_JWT_PASSWORD;
    }

    // As a final fallback for nonproduction/demo environments, return an empty string.
    // The backend lesson logic should be able to handle missing/invalid passwords gracefully.
    return '';
}

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        // Removed hardcoded password literal and delegate to getLoginPassword()
        data: JSON.stringify({ user: user, password: getLoginPassword() })
    }).done(function (response) {
        // Only store tokens if present; do not log them.
        if (response && typeof response === 'object') {
            if (response['access_token']) {
                localStorage.setItem('access_token', response['access_token']);
            }
            if (response['refresh_token']) {
                localStorage.setItem('refresh_token', response['refresh_token']);
            }
        }
    });
}

// Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    var token = localStorage.getItem('access_token');
    if (token) {
        headers_to_set['Authorization'] = 'Bearer ' + token;
    }
    return headers_to_set;
};

// Dev comment: Temporarily disabled from page we need to work out the refresh token flow but for now we can go live with the checkout page
function newToken() {
    var refreshToken = localStorage.getItem('refresh_token');
    $.ajax({
        headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('access_token')
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        data: JSON.stringify({ refreshToken: refreshToken })
    }).done(function (response) {
        // Expect updated tokens in the response; do not reference undefined globals
        if (response && typeof response === 'object') {
            if (response.access_token) {
                localStorage.setItem('access_token', response.access_token);
            }
            if (response.refresh_token) {
                localStorage.setItem('refresh_token', response.refresh_token);
            }
        }
    });
}
