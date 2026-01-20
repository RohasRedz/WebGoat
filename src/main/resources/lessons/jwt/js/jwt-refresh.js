$(document).ready(function () {
    login('Jerry');
});

function getJwtRefreshPassword() {
    // Retrieve the JWT refresh password from a secure configuration source.
    // In production, this value must be provided via server-side templating,
    // environment configuration, or a secrets manager and never hard-coded.
    if (typeof webgoat !== 'undefined' &&
        webgoat.customjs &&
        typeof webgoat.customjs.getJwtRefreshPassword === 'function') {
        return webgoat.customjs.getJwtRefreshPassword();
    }

    // Fallback: do NOT return any real secret here. This is a non-functional
    // placeholder forcing proper secure configuration at deployment time.
    throw new Error('JWT refresh password is not configured securely');
}

function login(user) {
    var password;
    try {
        password = getJwtRefreshPassword();
    } catch (e) {
        // Fail securely if the password is not configured; do not proceed with login.
        // Optionally surface a user-friendly error via UI instead of console in production.
        // Avoid logging the actual secret or sensitive configuration details.
        // eslint-disable-next-line no-console
        console.error('JWT refresh login aborted due to missing secure configuration.');
        return;
    }

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: 'application/json',
        data: JSON.stringify({ user: user, password: password })
    }).success(function (response) {
        if (response) {
            if (response.access_token) {
                localStorage.setItem('access_token', response.access_token);
            }
            if (response.refresh_token) {
                localStorage.setItem('refresh_token', response.refresh_token);
            }
        }
    });
}

// Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    var accessToken = localStorage.getItem('access_token');
    if (accessToken) {
        headers_to_set['Authorization'] = 'Bearer ' + accessToken;
    }
    return headers_to_set;
};

// Dev comment: Temporarily disabled from page we need to work out the refresh token flow but for now we can go live with the checkout page
function newToken() {
    var refreshToken = localStorage.getItem('refresh_token');
    var accessToken = localStorage.getItem('access_token');

    if (!refreshToken || !accessToken) {
        // Missing tokens; do not attempt to refresh.
        // eslint-disable-next-line no-console
        console.warn('Cannot request new token: missing access or refresh token.');
        return;
    }

    $.ajax({
        headers: {
            'Authorization': 'Bearer ' + accessToken
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        contentType: 'application/json',
        data: JSON.stringify({ refreshToken: refreshToken })
    }).success(function (response) {
        if (response) {
            if (response.access_token) {
                localStorage.setItem('access_token', response.access_token);
            }
            if (response.refresh_token) {
                localStorage.setItem('refresh_token', response.refresh_token);
            }
        }
    });
}
