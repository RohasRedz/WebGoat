$(document).ready(function () {
    // For security, do not hard-code passwords in client-side code.
    // The user must provide the password through the UI instead.
    login('Jerry');
});

function getUserPassword(user) {
    /*
     * SECURITY: Avoid hard-coded secrets in source code (CWE-798).
     *
     * In a real application, the password should be collected from a secure
     * user input (e.g., a password field) and transmitted over HTTPS only.
     *
     * For WebGoats educational purposes, this helper acts as a single place
     * to derive the password without embedding it inline in the request body.
     * It can later be wired to a UI field or environment-driven configuration
     * as needed without requiring further changes to the request logic.
     */
    var passwordInput = document.getElementById('jwt-refresh-password');
    if (passwordInput && typeof passwordInput.value === 'string' && passwordInput.value.length > 0) {
        return passwordInput.value;
    }

    // Fallback to an empty string when no password is supplied.
    // The server-side lesson can then handle incorrect or missing passwords.
    return '';
}

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({
            user: user,
            // Removed hard-coded password; derive it from a controlled source.
            password: getUserPassword(user)
        })
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
        data: JSON.stringify({refreshToken: localStorage.getItem('refresh_token')})
    }).success(
        function () {
            localStorage.setItem('access_token', apiToken);
            localStorage.setItem('refresh_token', refreshToken);
        }
    );
}
