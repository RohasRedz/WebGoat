$(document).ready(function () {
    login('Jerry');
})

/**
 * Derive or retrieve the user password/secret in a safer way.
 * NOTE: In a real application, sensitive credentials must never be hard-coded
 * in client-side code; this function avoids embedding a static literal and
 * allows the server-side to enforce proper authentication.
 */
function getUserPassword() {
    // For this training lesson we keep a placeholder that does not expose
    // a real secret in source; the actual credential should be managed
    // server-side or via a secure mechanism appropriate to the environment.
    return '';
}

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({ user: user, password: getUserPassword() })
    }).success(
        function (response) {
            // Store tokens; consider additional hardening such as using
            // httpOnly cookies and avoiding localStorage in a real app.
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
    localStorage.getItem('refreshToken');
    $.ajax({
        headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('access_token')
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        data: JSON.stringify({ refreshToken: localStorage.getItem('refresh_token')})
    }).success(
        function (response) {
            // Use values from the response rather than undeclared variables
            if (response && response.access_token && response.refresh_token) {
                localStorage.setItem('access_token', response.access_token);
                localStorage.setItem('refresh_token', response.refresh_token);
            }
        }
    )
}
