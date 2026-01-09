$(document).ready(function () {
    login('Jerry');
})

function login(user) {
    // Password is no longer hard-coded; it is expected to be provided
    // by a secure configuration mechanism on the server side.
    // The client should not contain any sensitive credentials.
    const passwordPlaceholder = '';

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        // Do not ship hard-coded secrets in client code.
        // The server endpoint must enforce its own authentication.
        data: JSON.stringify({ user: user, password: passwordPlaceholder })
    }).success(
        function (response) {
            // Store tokens in memory or secure storage if possible.
            // Here we preserve existing behavior but note that localStorage
            // is not ideal for long-lived sensitive tokens.
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
    // NOTE: `refreshToken` and `apiToken` must be supplied by the server;
// no hardcoded secrets should appear in this script.
    var currentRefreshToken = localStorage.getItem('refresh_token');
    $.ajax({
        headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('access_token')
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        data: JSON.stringify({ refreshToken: currentRefreshToken })
    }).success(
        function (response) {
            // Preserve behavior using values from the server response,
            // not any client-side constant:
            var newAccessToken = response['access_token'];
            var newRefreshToken = response['refresh_token'];

            localStorage.setItem('access_token', newAccessToken);
            localStorage.setItem('refresh_token', newRefreshToken);
        }
    )
}
