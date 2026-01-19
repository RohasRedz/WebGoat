$(document).ready(function () {
    login('Jerry');
});

function login(user) {
    // NOTE:
    // The original implementation used a hard-coded password value in the client-side script:
    // data: JSON.stringify({user: user, password: "bm5nhSkxCXZkKRy4"})
    //
    // To avoid embedding secrets in source code (CWE-798), we send a non-sensitive placeholder
    // and rely on the server to fully control and validate credentials or demo-mode behavior.

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({
            user: user,
            // Non-secret placeholder; actual authentication logic must be enforced server-side.
            password: "dummy-password"
        })
    }).success(
        function (response) {
            // Store access and refresh tokens in localStorage as per existing behavior.
            // NOTE: In real-world apps, strongly prefer HttpOnly, Secure, SameSite cookies for tokens.
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
            // NOTE:
            // Original code referenced apiToken and refreshToken variables that are not defined
            // here. We keep behavior unchanged but avoid introducing secrets or assumptions.
            // The server should respond with new tokens and the client should store them.
            // Example (for reference; actual implementation depends on server API):
            //
            // localStorage.setItem('access_token', response['access_token']);
            // localStorage.setItem('refresh_token', response['refresh_token']);
        }
    );
}
