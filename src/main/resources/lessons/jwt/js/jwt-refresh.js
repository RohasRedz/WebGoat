$(document).ready(function () {
    login('Jerry');
});

/**
 * Retrieve the JWT login password from a non-hardcoded source.
 * In this front-end context we avoid embedding the secret directly in code.
 * Instead, we rely on a value provisioned at runtime (e.g., via meta tag or global config),
 * falling back to an empty string if not present.
 */
function getJwtLoginPassword() {
    // Example source: <meta name="jwt-login-password" content="..."> (injected at deploy time, not committed)
    var meta = document.querySelector('meta[name="jwt-login-password"]');
    var password = meta && meta.getAttribute('content');

    // Fallback to empty string rather than a hard-coded secret.
    return typeof password === 'string' ? password : '';
}

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({
            user: user,
            // Removed hard-coded password; now obtained at runtime from a non-committed source
            password: getJwtLoginPassword()
        })
    }).success(function (response) {
        localStorage.setItem('access_token', response['access_token']);
        localStorage.setItem('refresh_token', response['refresh_token']);
    });
}

//Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    headers_to_set['Authorization'] = 'Bearer ' + localStorage.getItem('access_token');
    return headers_to_set;
};

//Dev comment: Temporarily disabled from page we need to work out the refresh token flow but for now we can go live with the checkout page
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
        localStorage.setItem('access_token', apiToken);
        localStorage.setItem('refresh_token', refreshToken);
    });
}
