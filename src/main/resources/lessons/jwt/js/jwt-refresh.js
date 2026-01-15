$(document).ready(function () {
    login('Jerry');
});

function login(user) {
    // Password moved to configuration/secret mechanism; not hard-coded in client JavaScript
    var password = getConfiguredJwtPassword();

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: 'application/json',
        data: JSON.stringify({ user: user, password: password })
    }).done(function (response) {
        // Store only what is required; tokens are still in localStorage to preserve existing behavior
        localStorage.setItem('access_token', response['access_token']);
        localStorage.setItem('refresh_token', response['refresh_token']);
    });
}

/**
 * Placeholder for secure configuration retrieval.
 * In production, this value must be injected server-side or derived from a secure configuration
 * mechanism, NOT hard-coded in client code or exposed to end users.
 */
function getConfiguredJwtPassword() {
    // Returning empty string here to avoid hardcoding secret in client.
    // Real secret must be provided via a secure, non-committed configuration mechanism.
    return '';
}

// Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    headers_to_set['Authorization'] = 'Bearer ' + localStorage.getItem('access_token');
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
        contentType: 'application/json',
        data: JSON.stringify({ refreshToken: refreshToken })
    }).done(function (response) {
        // Use values from server response instead of undeclared variables
        if (response && response.access_token && response.refresh_token) {
            localStorage.setItem('access_token', response.access_token);
            localStorage.setItem('refresh_token', response.refresh_token);
        }
    });
}
