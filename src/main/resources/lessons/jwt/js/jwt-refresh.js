$(document).ready(function () {
    // In this lesson flow, credentials must not be hard-coded in client-side code.
    // The user should be authenticated through the standard WebGoat login form.
    // Here we only demonstrate the token refresh mechanism; login() is no longer auto-invoked.
    // If needed for the lesson, the username can remain visible, but the password must not.
    // login('Jerry');
});

function login(user, password) {
    // Never hard-code passwords or secrets in client-side JavaScript.
    // In a real application, the password should come from a secure user input (e.g., HTML form)
    // sent over HTTPS, and not be stored or logged in client-side code.
    if (typeof password !== 'string' || !password) {
        // Fail fast if no password is provided; in WebGoat this is handled by the lesson UI.
        throw new Error('Password must be provided by the user; hard-coded passwords are not allowed.');
    }

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        // Send only the user-provided password; no secrets are embedded in the source code.
        data: JSON.stringify({ user: user, password: password })
    }).success(
        function (response) {
            // Store tokens as in the original behavior (lesson-specific).
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
    // Retrieve refresh token from localStorage (already stored in login()).
    var refreshToken = localStorage.getItem('refresh_token');
    $.ajax({
        headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('access_token')
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        data: JSON.stringify({ refreshToken: refreshToken })
    }).success(
        function (response) {
            // Update tokens with the values returned by the API instead of using undeclared variables.
            if (response && response.access_token && response.refresh_token) {
                localStorage.setItem('access_token', response.access_token);
                localStorage.setItem('refresh_token', response.refresh_token);
            }
        }
    );
}
