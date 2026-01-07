$(document).ready(function () {
    // For this training/demo, prompt the user for the password at runtime
    // instead of hard-coding any credential in the source.
    login('Jerry');
});

/**
 * Retrieve the user's password at runtime without hard-coding it in source.
 *
 * NOTE: This is for training/demo purposes only. Real applications MUST:
 * - Never handle plaintext passwords in client-side JavaScript.
 * - Collect passwords via secure form fields over HTTPS and send them
 *   directly to the server for verification.
 */
function getUserPassword(user) {
    // Prompt the user for the password. This avoids any hard-coded secret
    // in the JavaScript source while keeping the behavior functional.
    // In a real application, use an <input type="password"> in a form
    // instead of window.prompt, and never store the password in JS variables
    // longer than necessary.
    var password = window.prompt('Enter password for user ' + user + ':', '');
    if (typeof password !== 'string') {
        return '';
    }
    // Basic normalization; do not trim in real apps if whitespace is meaningful.
    return password;
}

function login(user) {
    var password = getUserPassword(user);

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({ user: user, password: password })
    }).success(
        function (response) {
            localStorage.setItem('access_token', response['access_token']);
            localStorage.setItem('refresh_token', response['refresh_token']);
        }
    );
}

//Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    headers_to_set['Authorization'] = 'Bearer ' + localStorage.getItem('access_token');
    return headers_to_set;
}

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
    }).success(
        function () {
            localStorage.setItem('access_token', apiToken);
            localStorage.setItem('refresh_token', refreshToken);
        }
    );
}
