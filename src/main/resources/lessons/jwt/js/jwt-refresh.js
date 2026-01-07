$(document).ready(function () {
    // Removed automatic login with hard-coded user/password to avoid hard-coded secrets.
    // If automatic login is desired for a demo environment, ensure credentials come from
    // a non-hardcoded, configurable source instead.
});

/**
 * Perform login using credentials supplied from the page (secure input fields)
 * instead of hard-coded values in source code.
 *
 * Expects the HTML page to provide:
 *   <input type="text" id="jwt-username" ...>
 *   <input type="password" id="jwt-password" ...>
 */
function login() {
    var user = $('#jwt-username').val();
    var password = $('#jwt-password').val();

    // Basic client-side presence check; full validation and authentication must be on server.
    if (!user || !password) {
        // In a real app, surface this to the user via the UI instead of alert().
        // Here we keep behavior simple and non-verbose.
        return;
    }

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({ user: user, password: password })
    }).success(
        function (response) {
            // Store tokens as before; for real applications, consider more secure storage
            // (e.g., HttpOnly cookies issued by the server).
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
    // Preserve existing behavior but ensure we read tokens from storage, not constants.
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
            // Assume server returns updated tokens named access_token / refresh_token
            if (response && response['access_token'] && response['refresh_token']) {
                localStorage.setItem('access_token', response['access_token']);
                localStorage.setItem('refresh_token', response['refresh_token']);
            }
        }
    );
}
