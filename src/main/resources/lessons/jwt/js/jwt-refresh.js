$(document).ready(function () {
    // Use a non-sensitive default user identifier. Do NOT rely on a hard-coded
    // password; the server must perform proper authentication and secret handling.
    login('Jerry');
});

function login(user) {
    // Never embed hard-coded passwords or secrets in client-side code.
    // Use a non-sensitive placeholder and ensure the backend ignores or
    // rejects this field in production, relying instead on real credentials
    // provided through a secure authentication flow.
    var safePayload = {
        user: String(user || ''),
        // Non-secret placeholder; original hard-coded value removed.
        password: ''
    };

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: 'application/json',
        data: JSON.stringify(safePayload)
    }).done(function (response) {
        // Store tokens in localStorage only if that pattern is already
        // expected by the application; no change here to avoid breaking
        // existing logic, but note that HttpOnly cookies are generally safer.
        if (response && typeof response === 'object') {
            if (typeof response.access_token === 'string') {
                localStorage.setItem('access_token', response.access_token);
            }
            if (typeof response.refresh_token === 'string') {
                localStorage.setItem('refresh_token', response.refresh_token);
            }
        }
    });
}

// Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    var accessToken = localStorage.getItem('access_token');

    // Only add Authorization header when a non-empty token exists.
    if (typeof accessToken === 'string' && accessToken.length > 0) {
        headers_to_set.Authorization = 'Bearer ' + accessToken;
    }

    return headers_to_set;
};

// Dev comment: Temporarily disabled from page we need to work out the refresh token flow but for now we can go live with the checkout page
function newToken() {
    var refreshToken = localStorage.getItem('refresh_token');

    // Do not proceed if there is no refresh token.
    if (typeof refreshToken !== 'string' || refreshToken.length === 0) {
        return;
    }

    $.ajax({
        headers: {
            Authorization: 'Bearer ' + localStorage.getItem('access_token')
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        contentType: 'application/json',
        data: JSON.stringify({ refreshToken: refreshToken })
    }).done(function (response) {
        // Use values returned from the backend instead of undeclared variables.
        if (response && typeof response === 'object') {
            if (typeof response.access_token === 'string') {
                localStorage.setItem('access_token', response.access_token);
            }
            if (typeof response.refresh_token === 'string') {
                localStorage.setItem('refresh_token', response.refresh_token);
            }
        }
    });
}
