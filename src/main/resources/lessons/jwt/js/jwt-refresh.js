$(document).ready(function () {
    login('Jerry');
});

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        // FIX: Remove hard-coded password from client-side source; use a placeholder and
        // require the backend to enforce proper authentication flows.
        //
        // The password field is now intentionally left empty here. In a real deployment,
        // the password MUST be collected securely from the user (e.g., via a secure form)
        // and transmitted over HTTPS only. This demo code no longer exposes any
        // hard-coded credential in the source.
        data: JSON.stringify({ user: user, password: "" })
    }).success(
        function (response) {
            // Store tokens in localStorage as per existing app behavior.
            // NOTE: In a production system, consider using httpOnly cookies instead
            // for access/refresh tokens to reduce XSS impact.
            if (response && typeof response === 'object') {
                if (response['access_token']) {
                    localStorage.setItem('access_token', response['access_token']);
                }
                if (response['refresh_token']) {
                    localStorage.setItem('refresh_token', response['refresh_token']);
                }
            }
        }
    );
}

//Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    var accessToken = localStorage.getItem('access_token');

    // Defensive: only add header if token is present
    if (accessToken) {
        headers_to_set['Authorization'] = 'Bearer ' + accessToken;
    }
    return headers_to_set;
};

//Dev comment: Temporarily disabled from page we need to work out the refresh token flow
// but for now we can go live with the checkout page
function newToken() {
    var refreshToken = localStorage.getItem('refresh_token');

    $.ajax({
        headers: (function () {
            var headers = {};
            var currentAccessToken = localStorage.getItem('access_token');
            if (currentAccessToken) {
                headers['Authorization'] = 'Bearer ' + currentAccessToken;
            }
            return headers;
        })(),
        type: 'POST',
        url: 'JWT/refresh/newToken',
        contentType: "application/json",
        data: JSON.stringify({ refreshToken: refreshToken })
    }).success(
        function (response) {
            // Use tokens returned from server response rather than undefined variables
            if (response && typeof response === 'object') {
                if (response.access_token) {
                    localStorage.setItem('access_token', response.access_token);
                }
                if (response.refresh_token) {
                    localStorage.setItem('refresh_token', response.refresh_token);
                }
            }
        }
    );
}
