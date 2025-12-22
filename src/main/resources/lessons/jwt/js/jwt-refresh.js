$(document).ready(function () {
    // Expect a non-empty password/secret to be provided at runtime.
    // This avoids hard-coding secrets in the client bundle.
    var runtimePassword = window.WEBGOAT_JWT_PASSWORD;

    if (!runtimePassword || typeof runtimePassword !== 'string') {
        // Fallback to a non-secret placeholder to avoid breaking the example flow,
        // while not leaking real credentials.
        runtimePassword = 'PLACEHOLDER_PASSWORD';
    }

    login('Jerry', runtimePassword);
})

function login(user, password) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        // Do not hard-code secrets in source; use the runtime-supplied password instead.
        data: JSON.stringify({user: user, password: password})
    }).success(
        function (response) {
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
        data: JSON.stringify({refreshToken: localStorage.getItem('refresh_token')})
    }).success(
        function () {
            localStorage.setItem('access_token', apiToken);
            localStorage.setItem('refresh_token', refreshToken);
        }
    )
}
