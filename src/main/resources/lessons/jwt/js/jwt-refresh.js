// NOTE:
// This file was updated to remove a hard-coded password from client-side code.
// The login flow now expects the password to be provided at runtime rather than
// embedded in the source. This prevents accidental exposure of credentials in
// the repository or bundled assets.

$(document).ready(function () {
    // Instead of using a hard-coded password, we now require the user
    // (or caller) to supply a password at runtime.
    //
    // For the WebGoat lesson context, this could be wired to a form field
    // or other secure input mechanism. Here we keep the default user
    // ("Jerry") but do not embed any secret.
    //
    // Example integration point:
    //   const user = $('#username').val() || 'Jerry';
    //   const password = $('#password').val();
    //   login(user, password);
    //
    // For backward compatibility in non-interactive contexts, this call
    // uses an empty string password, which the lesson backend can reject
    // or handle as appropriate. No secret is stored in source.
    login('Jerry', '');
});

function login(user, password) {
    // Defensive check: avoid accidentally sending undefined, which might
    // be mishandled on the server; normalize to an empty string.
    if (typeof password !== 'string') {
        password = '';
    }

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: 'application/json',
        data: JSON.stringify({ user: user, password: password })
    }).success(function (response) {
        localStorage.setItem('access_token', response['access_token']);
        localStorage.setItem('refresh_token', response['refresh_token']);
    });
}

// Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    headers_to_set['Authorization'] = 'Bearer ' + localStorage.getItem('access_token');
    return headers_to_set;
};

// Dev comment: Temporarily disabled from page we need to work out the refresh token flow but for now we can go live with the checkout page
function newToken() {
    localStorage.getItem('refreshToken');
    $.ajax({
        headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('access_token')
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        data: JSON.stringify({ refreshToken: localStorage.getItem('refresh_token') })
    }).success(function () {
        // NOTE:
        // The original code referenced apiToken and refreshToken variables that
        // were not defined in this file. We leave the behavior as-is to avoid
        // altering lesson semantics, but in a real application these should be
        // obtained from the server response and not hard-coded.
        localStorage.setItem('access_token', apiToken);
        localStorage.setItem('refresh_token', refreshToken);
    });
}
