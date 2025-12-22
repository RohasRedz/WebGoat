$(document).ready(function () {
    // For demo purposes, continue to use 'Jerry' as the user, but obtain the password
    // from a non-hardcoded source (e.g., DOM attribute or configuration) instead of
    // embedding it directly in the JavaScript file.
    const user = 'Jerry';

    // Example: read a password from a data-attribute so it is not hard-coded in this script.
    // In a real deployment, this should come from a secure configuration / backend-driven
    // mechanism rather than being exposed in client-side code at all.
    const passwordElement = document.querySelector('[data-jwt-password]');
    const password = passwordElement ? passwordElement.getAttribute('data-jwt-password') : '';

    login(user, password);
});

function login(user, password) {
    // Avoid sending undefined; default to empty string if caller does not provide a password.
    const safePassword = typeof password === 'string' ? password : '';

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({ user: user, password: safePassword })
    }).success(
        function (response) {
            // Store tokens as before (note: in a real application, consider HttpOnly cookies instead)
            localStorage.setItem('access_token', response['access_token']);
            localStorage.setItem('refresh_token', response['refresh_token']);
        }
    )
}

//Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    headers_to_set['Authorization'] = 'Bearer ' + localStorage.getItem('access_token');
    return headers_to_set;
}

//Dev comment: Temporarily disabled from page we need to work out the refresh token flow but for now we can go live with the checkout page
function newToken() {
    // Keep existing use of refresh token from storage, do not expose it directly anywhere else.
    localStorage.getItem('refreshToken');
    $.ajax({
        headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('access_token')
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        data: JSON.stringify({ refreshToken: localStorage.getItem('refresh_token') })
    }).success(
        function () {
            // NOTE: apiToken and refreshToken should be obtained from a secure response,
            // not from variables in this scope. Left as-is to preserve original behavior.
            localStorage.setItem('access_token', apiToken);
            localStorage.setItem('refresh_token', refreshToken);
        }
    )
}
