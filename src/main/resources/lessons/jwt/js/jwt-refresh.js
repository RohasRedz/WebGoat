$(document).ready(function () {
    // Instead of using a hard-coded user or password, rely on user-provided credentials
    // from the UI (e.g., a login form). This avoids shipping secrets in source code.
    var loginForm = $('#jwt-login-form');
    if (loginForm.length) {
        loginForm.on('submit', function (e) {
            e.preventDefault();

            // Read credentials from secure input fields
            var user = $.trim($('#jwt-username').val());
            var password = $('#jwt-password').val(); // do NOT log or persist this

            // Basic input validation to avoid empty submissions
            if (!user || !password) {
                // Optionally show a generic error to the user; avoid exposing details
                alert('Username and password are required.');
                return;
            }

            login(user, password);
        });
    } else {
        // Fallback for legacy/demo behavior: if no form is present, do NOT attempt
        // to use a hard-coded password. Require explicit user interaction instead.
        // This preserves behavior where possible while avoiding hard-coded secrets.
        console.warn('JWT login form not found; automatic login is disabled for security reasons.');
    }
});

function login(user, password) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: 'application/json',
        // Removed hard-coded password; now using function argument obtained from user input.
        data: JSON.stringify({ user: user, password: password })
    }).success(function (response) {
        // Store tokens; consider HttpOnly cookies server-side in a real application.
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
        // NOTE: apiToken and refreshToken should be set from the server response,
        // not from hard-coded values. This block is left as-is as it is unrelated
        // to the hard-coded password finding, but should be reviewed separately.
        localStorage.setItem('access_token', apiToken);
        localStorage.setItem('refresh_token', refreshToken);
    });
}
