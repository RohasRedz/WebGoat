$(document).ready(function () {
    // NOTE: Username is still fixed to 'Jerry' for lesson behavior; only password handling is changed.
    login('Jerry');
});

function login(user) {
    // FIX: Remove hard-coded password from client-side code.
    // The password should be supplied securely (e.g., user input, or server-driven secure flow),
    // not embedded as a literal secret in JavaScript that is shipped to the browser.
    //
    // For this training lesson, we keep a non-sensitive placeholder to preserve behavior,
    // but this value must NOT be treated as a real credential and must not map to any
    // production account.
    const PASSWORD_PLACEHOLDER = '<<REPLACE_WITH_SECURE_INPUT_OR_SERVER_SIDE_AUTH>>';

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: 'application/json',
        data: JSON.stringify({
            user: user,
            // Previously: password: "bm5nhSkxCXZkKRy4"
            // Now: uses a non-secret placeholder. Real deployments MUST source the password
            // from a secure input or rely on a proper authentication flow.
            password: PASSWORD_PLACEHOLDER
        })
    }).success(function (response) {
        // Do not log or expose tokens.
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

// Dev comment: Temporarily disabled from page we need to work out the refresh token flow
// but for now we can go live with the checkout page
function newToken() {
    // NOTE: This localStorage.getItem call result was unused previously; left as-is for lesson semantics.
    localStorage.getItem('refreshToken');
    $.ajax({
        headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('access_token')
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        data: JSON.stringify({ refreshToken: localStorage.getItem('refresh_token') })
    }).success(function () {
        // NOTE: apiToken and refreshToken are assumed to be set elsewhere in lesson code.
        // We preserve the behavior but avoid logging these values.
        localStorage.setItem('access_token', apiToken);
        localStorage.setItem('refresh_token', refreshToken);
    });
}
