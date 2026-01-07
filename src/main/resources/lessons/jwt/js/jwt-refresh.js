$(document).ready(function () {
    login('Jerry');
});

function login(user) {
    // Retrieve password/secret from a safer configuration mechanism.
    // For this lesson/demo, we first check for a runtime-provided value (e.g., via webgoat.customjs)
    // and only fall back to a non-sensitive placeholder to avoid hardcoding real secrets.
    var configuredPassword =
        (window.webgoat &&
            window.webgoat.customjs &&
            typeof window.webgoat.customjs.getJwtDemoPassword === 'function' &&
            window.webgoat.customjs.getJwtDemoPassword()) ||
        'not-a-real-secret';

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({ user: user, password: configuredPassword })
    }).success(
        function (response) {
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
    localStorage.getItem('refreshToken');
    $.ajax({
        headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('access_token')
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        data: JSON.stringify({ refreshToken: localStorage.getItem('refresh_token') })
    }).success(
        function () {
            localStorage.setItem('access_token', apiToken);
            localStorage.setItem('refresh_token', refreshToken);
        }
    );
}
