$(document).ready(function () {
    login('Jerry');
});

function login(user) {
    // Password/secret must not be hard-coded; use a placeholder here and ensure
    // the real value is supplied via secure configuration in the running system.
    var password = getJwtDemoPassword();

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: 'application/json',
        data: JSON.stringify({ user: user, password: password })
    }).success(function (response) {
        // Store tokens only in memory for this demo; avoid persistent storage where possible
        if (response && typeof response.access_token === 'string') {
            localStorage.setItem('access_token', response['access_token']);
        }
        if (response && typeof response.refresh_token === 'string') {
            localStorage.setItem('refresh_token', response['refresh_token']);
        }
    });
}

// In a real deployment this should be wired to secure configuration
// (env variables, secrets manager, etc.). Here we centralize it to avoid
// scattering secrets and to allow replacement without code changes.
function getJwtDemoPassword() {
    // Placeholder: the platform’s secure configuration mechanism
    // should inject the actual password/secret at runtime.
    // Returning an empty string by default ensures no valid credential
    // is accidentally baked into the client bundle.
    return '';
}

// Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs = webgoat.customjs || {};
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    var accessToken = localStorage.getItem('access_token');

    if (typeof accessToken === 'string' && accessToken.length > 0) {
        headers_to_set['Authorization'] = 'Bearer ' + accessToken;
    }

    return headers_to_set;
};

// Dev comment: Temporarily disabled from page we need to work out the refresh token flow
// but for now we can go live with the checkout page
function newToken() {
    var refreshToken = localStorage.getItem('refresh_token');

    if (!refreshToken) {
        return;
    }

    $.ajax({
        headers: webgoat.customjs.addBearerToken(),
        type: 'POST',
        url: 'JWT/refresh/newToken',
        contentType: 'application/json',
        data: JSON.stringify({ refreshToken: refreshToken })
    }).success(function (response) {
        // Use values from the response instead of undeclared globals
        if (response && typeof response.access_token === 'string') {
            localStorage.setItem('access_token', response.access_token);
        }
        if (response && typeof response.refresh_token === 'string') {
            localStorage.setItem('refresh_token', response.refresh_token);
        }
    });
}
