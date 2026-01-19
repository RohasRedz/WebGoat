$(document).ready(function () {
    login('Jerry');
});

function login(user) {
    // Read the password/secret from a non-hardcoded source to avoid embedded credentials.
    // For this lesson context, we fall back to a placeholder if not configured.
    var password = (typeof webgoat !== 'undefined' &&
        webgoat.config &&
        webgoat.config.jwtPassword) || '';

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({ user: user, password: password })
    }).success(
        function (response) {
            // Store only what is necessary; note that in a real app,
            // tokens in localStorage have security tradeoffs (XSS exposure).
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
    // Correct key used to read the refresh token
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
            // Use values returned by the API instead of undefined variables
            if (response && response.access_token && response.refresh_token) {
                localStorage.setItem('access_token', response.access_token);
                localStorage.setItem('refresh_token', response.refresh_token);
            }
        }
    );
}
