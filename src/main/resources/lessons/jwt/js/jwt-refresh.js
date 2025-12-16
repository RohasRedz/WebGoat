$(document).ready(function () {
    login('Jerry');
});

function login(user) {
    // Password is no longer hard-coded in the source.
    // It is expected to be provided via a secure configuration mechanism
    // (e.g., injected at build time, environment variable, or separate config file).
    var password = webgoat && webgoat.config && typeof webgoat.config.getJwtRefreshPassword === 'function'
        ? webgoat.config.getJwtRefreshPassword()
        : null;

    if (typeof password !== 'string' || !password.length) {
        // Fail securely on missing or invalid password configuration.
        // Do NOT log the actual password or other secrets.
        // A generic alert is acceptable in this demo context.
        console.error('JWT refresh login password is not configured securely.');
        return;
    }

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: 'application/json',
        data: JSON.stringify({ user: user, password: password })
    }).success(function (response) {
        if (!response) {
            console.error('Empty response from JWT refresh login endpoint.');
            return;
        }

        // Only store non-sensitive tokens; do not log or expose them.
        if (typeof response.access_token === 'string') {
            localStorage.setItem('access_token', response.access_token);
        } else {
            console.error('Missing access_token in JWT refresh login response.');
        }

        if (typeof response.refresh_token === 'string') {
            localStorage.setItem('refresh_token', response.refresh_token);
        } else {
            console.error('Missing refresh_token in JWT refresh login response.');
        }
    }).error(function (jqXHR, textStatus) {
        // Generic error logging without leaking sensitive details.
        console.error('JWT refresh login request failed:', textStatus);
    });
}

// Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    var accessToken = localStorage.getItem('access_token');

    if (typeof accessToken === 'string' && accessToken.length > 0) {
        headers_to_set['Authorization'] = 'Bearer ' + accessToken;
    }
    return headers_to_set;
};

// Dev comment: Temporarily disabled from page we need to work out the refresh token flow but for now we can go live with the checkout page
function newToken() {
    var refreshToken = localStorage.getItem('refresh_token');
    if (typeof refreshToken !== 'string' || !refreshToken.length) {
        console.error('No refresh token available for JWT refresh flow.');
        return;
    }

    $.ajax({
        headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('access_token')
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        contentType: 'application/json',
        data: JSON.stringify({ refreshToken: refreshToken })
    }).success(function (response) {
        if (!response) {
            console.error('Empty response from JWT refresh newToken endpoint.');
            return;
        }

        // Use response values instead of undefined variables; never log them.
        if (typeof response.access_token === 'string') {
            localStorage.setItem('access_token', response.access_token);
        } else {
            console.error('Missing access_token in newToken response.');
        }

        if (typeof response.refresh_token === 'string') {
            localStorage.setItem('refresh_token', response.refresh_token);
        } else {
            console.error('Missing refresh_token in newToken response.');
        }
    }).error(function (jqXHR, textStatus) {
        console.error('JWT refresh newToken request failed:', textStatus);
    });
}
