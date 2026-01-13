$(document).ready(function () {
    login('Jerry');
});

function login(user) {
    // NOTE:
    // The password value should NOT be hard-coded in source.
    // This implementation expects a secure injection of the password via a global
    // configuration object or environment mapping set up by the platform.
    // If the secure configuration is missing, the call is safely aborted.
    var password = (window.webgoatConfig &&
        typeof window.webgoatConfig.getJwtRefreshPassword === 'function')
        ? window.webgoatConfig.getJwtRefreshPassword()
        : null;

    if (!password) {
        // Fail securely if no configured password is available.
        // Do NOT log the actual password or sensitive values.
        if (window.console && typeof window.console.error === 'function') {
            console.error('JWT refresh login password is not configured securely.');
        }
        return;
    }

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({ user: user, password: password })
    }).success(
        function (response) {
            if (response && typeof response === 'object') {
                if (typeof response['access_token'] === 'string') {
                    localStorage.setItem('access_token', response['access_token']);
                }
                if (typeof response['refresh_token'] === 'string') {
                    localStorage.setItem('refresh_token', response['refresh_token']);
                }
            }
        }
    );
}

//Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    var accessToken = localStorage.getItem('access_token');
    if (typeof accessToken === 'string' && accessToken.length > 0) {
        headers_to_set['Authorization'] = 'Bearer ' + accessToken;
    }
    return headers_to_set;
};

//Dev comment: Temporarily disabled from page we need to work out the refresh token flow but for now we can go live with the checkout page
function newToken() {
    var refreshToken = localStorage.getItem('refresh_token');
    if (!refreshToken) {
        if (window.console && typeof window.console.warn === 'function') {
            console.warn('No refresh token available for newToken request.');
        }
        return;
    }

    $.ajax({
        headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('access_token')
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        contentType: "application/json",
        data: JSON.stringify({ refreshToken: refreshToken })
    }).success(
        function (response) {
            // Expect the API to return new tokens; update only if present.
            if (response && typeof response === 'object') {
                if (typeof response.accessToken === 'string') {
                    localStorage.setItem('access_token', response.accessToken);
                }
                if (typeof response.refreshToken === 'string') {
                    localStorage.setItem('refresh_token', response.refreshToken);
                }
            }
        }
    );
}
