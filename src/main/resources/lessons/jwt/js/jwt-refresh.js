$(document).ready(function () {
    // In a real deployment, user identity should come from authenticated context,
    // but we retain existing behavior for the lesson while removing hard-coded secrets.
    login('Jerry');
});

function login(user) {
    // Password removed from client-side code to avoid hard-coded secret exposure.
    // In production, the server should not rely on a static password supplied by the client at all.
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        // Send no password from client – server-side lesson code should handle this appropriately.
        data: JSON.stringify({ user: user })
    }).success(
        function (response) {
            // Store tokens in localStorage as per existing application design.
            // NOTE: For production applications, consider HttpOnly secure cookies instead.
            localStorage.setItem('access_token', response['access_token']);
            localStorage.setItem('refresh_token', response['refresh_token']);
        }
    );
}

// Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    headers_to_set['Authorization'] = 'Bearer ' + localStorage.getItem('access_token');
    return headers_to_set;
};

// Dev comment: Temporarily disabled from page we need to work out the refresh token flow but for now we can go live with the checkout page
function newToken() {
    // NOTE: This function appears unused/disabled as per comment; we keep behavior but avoid introducing new secrets.
    var refreshToken = localStorage.getItem('refresh_token');

    $.ajax({
        headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('access_token')
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        contentType: "application/json",
        data: JSON.stringify({ refreshToken: refreshToken })
    }).success(
        function (response) {
            // Use response tokens instead of undeclared variables (apiToken / refreshToken).
            if (response && response.access_token && response.refresh_token) {
                localStorage.setItem('access_token', response.access_token);
                localStorage.setItem('refresh_token', response.refresh_token);
            }
        }
    );
}
