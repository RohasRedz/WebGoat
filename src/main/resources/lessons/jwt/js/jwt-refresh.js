$(document).ready(function () {
    login('Jerry');
})

function getJwtDemoPassword() {
    // In a real deployment, this value must NOT be hard-coded and must be provided
    // via a secure configuration source (e.g., environment variable -> server -> window.webgoatConfig).
    if (typeof window.webgoatConfig !== 'undefined' &&
        window.webgoatConfig !== null &&
        typeof window.webgoatConfig.jwtDemoPassword === 'string' &&
        window.webgoatConfig.jwtDemoPassword.length > 0) {
        return window.webgoatConfig.jwtDemoPassword;
    }

    // SECURITY HARDENING:
    // We no longer provide any valid secret as a hard-coded fallback.
    // This placeholder clearly indicates misconfiguration and MUST NOT be used as a real credential.
    return "CONFIGURE_JWT_DEMO_PASSWORD";
}

function login(user) {
    var password = getJwtDemoPassword();

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
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
