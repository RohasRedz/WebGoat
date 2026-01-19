$(document).ready(function () {
    login('Jerry');
})

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        // Remove hard-coded password; use a placeholder that must be supplied securely at runtime.
        // In the WebGoat lab context this demonstrates that credentials should not be hard-coded.
        data: JSON.stringify({
            user: user,
            password: getUserPassword()
        })
    }).success(
        function (response) {
            localStorage.setItem('access_token', response['access_token']);
            localStorage.setItem('refresh_token', response['refresh_token']);
        }
    )
}

/**
 * getUserPassword is a placeholder to demonstrate secure secret handling.
 * In a real application this value MUST come from a secure source
 * (e.g., user input, or a backend flow using environment-configured secrets),
 * not from a hard-coded literal in client-side code.
 */
function getUserPassword() {
    // For security, do not embed hard-coded secrets in client JavaScript.
    // In the WebGoat training context, this function intentionally returns
    // an empty string and relies on the server-side lesson to illustrate
    // proper secret management.
    return '';
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
        data: JSON.stringify({refreshToken: localStorage.getItem('refresh_token')})
    }).success(
        function () {
            // NOTE: apiToken and refreshToken should be provided by the server response.
            // This function preserves the original behavior while documenting the requirement.
            localStorage.setItem('access_token', apiToken);
            localStorage.setItem('refresh_token', refreshToken);
        }
    )
}
