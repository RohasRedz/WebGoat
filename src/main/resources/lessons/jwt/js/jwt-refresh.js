$(document).ready(function () {
    login('Jerry');
});

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: 'application/json',
        // FIX: do not hard-code passwords in source; send a non-sensitive placeholder or rely on user-provided credentials.
        // The real secret should be obtained server-side or from a secure configuration mechanism, not embedded in JS.
        data: JSON.stringify({ user: user, password: '' })
    }).done(function (response) {
        // Store tokens in memory-like variables instead of localStorage to reduce persistence risk.
        // In a real app, tokens should be handled via secure, HttpOnly cookies or other hardened mechanisms.
        window.webgoat = window.webgoat || {};
        window.webgoat.tokens = window.webgoat.tokens || {};

        window.webgoat.tokens.access_token = response['access_token'];
        window.webgoat.tokens.refresh_token = response['refresh_token'];
    });
}

// Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    window.webgoat = window.webgoat || {};
    window.webgoat.tokens = window.webgoat.tokens || {};

    var headers_to_set = {};
    headers_to_set['Authorization'] = 'Bearer ' + (window.webgoat.tokens.access_token || '');
    return headers_to_set;
};

// Dev comment: Temporarily disabled from page we need to work out the refresh token flow but for now we can go live with the checkout page
function newToken() {
    window.webgoat = window.webgoat || {};
    window.webgoat.tokens = window.webgoat.tokens || {};

    var refreshToken = window.webgoat.tokens.refresh_token || '';

    $.ajax({
        headers: {
            'Authorization': 'Bearer ' + (window.webgoat.tokens.access_token || '')
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        contentType: 'application/json',
        data: JSON.stringify({ refreshToken: refreshToken })
    }).done(function (response) {
        // Update tokens based on server response; avoid using undefined variables.
        if (response && response.access_token && response.refresh_token) {
            window.webgoat.tokens.access_token = response.access_token;
            window.webgoat.tokens.refresh_token = response.refresh_token;
        }
    });
}
