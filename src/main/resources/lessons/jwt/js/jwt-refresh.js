$(document).ready(function () {
    // Use a non-sensitive placeholder value in client-side demo code.
    // Real credentials must be provided securely by the user, not hard-coded.
    login('Jerry', 'demo-password');
});

function login(user, password) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        // Do not hard-code real passwords or secrets in source code.
        // In a real deployment, `password` should come from a secure user input flow.
        data: JSON.stringify({ user: user, password: password })
    }).success(
        function (response) {
            // Store tokens as provided by the backend; avoid logging or exposing them.
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

// Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    headers_to_set['Authorization'] = 'Bearer ' + localStorage.getItem('access_token');
    return headers_to_set;
};

// Dev comment: Temporarily disabled from page we need to work out the refresh token flow but for now we can go live with the checkout page
function newToken() {
    // Use the stored refresh_token value; do not introduce new hardcoded secrets.
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
            // Update tokens only from backend response; avoid using undefined variables.
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
