$(document).ready(function () {
    login('Jerry');
});

function login(user) {
    // The password value is now read from a secure configuration source at runtime
    // and is not hard-coded in the client code. In production, this would be
    // provided by the backend or a secure config mechanism, never baked into JS.
    var password = window.webgoat && typeof window.webgoat.getJwtDemoPassword === 'function'
        ? window.webgoat.getJwtDemoPassword()
        : '';

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({ user: user, password: password })
    }).success(
        function (response) {
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
    // Ensure we consistently use the stored refresh_token and do not rely on undefined globals
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
            // Prefer using tokens returned from the backend instead of undefined globals
            if (response && response.access_token) {
                localStorage.setItem('access_token', response.access_token);
            }
            if (response && response.refresh_token) {
                localStorage.setItem('refresh_token', response.refresh_token);
            }
        }
    );
}
