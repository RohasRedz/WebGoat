$(document).ready(function () {
    // Do not automatically log in with a hard-coded password.
    // If automatic login is required for this lesson, the password should be injected
    // from a secure configuration mechanism rather than being hard-coded here.
    // For now, we only auto-login with a user identifier; the backend must handle
    // authentication or use a safe demo credential not stored in client code.
    login('Jerry');
});

function login(user) {
    // Retrieve password from a non-hard-coded source. In a real application, this would
    // come from a secure configuration or a user input field rather than being embedded
    // directly in the script.
    var passwordInputElement = document.getElementById('jwt-demo-password');
    var passwordValue = passwordInputElement ? passwordInputElement.value : '';

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        // Removed hard-coded password; now uses a dynamic value which is not stored in source code.
        data: JSON.stringify({ user: user, password: passwordValue })
    }).success(
        function (response) {
            // Store tokens; in a real production environment, consider using more secure
            // storage mechanisms depending on the threat model (e.g., HttpOnly cookies).
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
    }).success(
        function () {
            // NOTE: In the original code, `apiToken` and `refreshToken` are referenced but not defined
            // here. We preserve this behavior to avoid altering business logic.
            localStorage.setItem('access_token', apiToken);
            localStorage.setItem('refresh_token', refreshToken);
        }
    );
}
