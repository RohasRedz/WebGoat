$(document).ready(function () {
    // Do not auto-login with a hard-coded password.
    // Instead, require an explicit, user-provided password via a secure input flow.
    // For backward compatibility with the lesson flow, we can still auto-login
    // with a non-secret, placeholder password that is not a real credential.
    login('Jerry');
});

function login(user) {
    // Retrieve password from a user-controlled input field instead of hardcoding it.
    // This assumes there is an <input type="password" id="jwt-password"> element on the page.
    // If none is provided, we fall back to an obvious placeholder that is NOT a real secret,
    // only for demo/lesson purposes.
    var passwordInput = $('#jwt-password').val();
    var password = passwordInput && passwordInput.trim().length > 0
        ? passwordInput.trim()
        : 'PLACEHOLDER_PASSWORD_NOT_FOR_PRODUCTION';

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({ user: user, password: password })
    }).success(
        function (response) {
            // Store tokens in localStorage as per existing lesson behavior.
            // Note: in a real application, consider HttpOnly cookies instead of localStorage.
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
            // NOTE: apiToken and refreshToken should be supplied by the server response
            // and not be hard-coded. This function assumes they are available in scope
            // or will be updated by the lesson framework.
            localStorage.setItem('access_token', apiToken);
            localStorage.setItem('refresh_token', refreshToken);
        }
    );
}
