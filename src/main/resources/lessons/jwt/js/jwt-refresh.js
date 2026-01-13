$(document).ready(function () {
    login('Jerry');
});

function login(user) {
    // NOTE: Password must not be hard-coded in source.
    // In a real deployment, this value should be provided from a secure source
    // (e.g., environment variable, secrets manager) and never exposed to clients.
    const password = window.webgoatConfig && window.webgoatConfig.jwtDemoPassword
        ? window.webgoatConfig.jwtDemoPassword
        : '';

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({ user: user, password: password })
    }).success(
        function (response) {
            // Only store tokens if they are present and non-empty
            if (response && typeof response.access_token === 'string' && response.access_token.length > 0) {
                localStorage.setItem('access_token', response['access_token']);
            }
            if (response && typeof response.refresh_token === 'string' && response.refresh_token.length > 0) {
                localStorage.setItem('refresh_token', response['refresh_token']);
            }
        }
    );
}

//Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    var accessToken = localStorage.getItem('access_token');
    if (typeof accessToken === 'string' && accessToken.length > 0) {
        headers_to_set['Authorization'] = 'Bearer ' + accessToken;
    }
    return headers_to_set;
};

//Dev comment: Temporarily disabled from page we need to work out the refresh token flow but for now we can go live with the checkout page
function newToken() {
    // Use the stored refresh token safely without logging it anywhere
    var refreshToken = localStorage.getItem('refresh_token');
    var accessToken = localStorage.getItem('access_token');

    if (!refreshToken || !accessToken) {
        return;
    }

    $.ajax({
        headers: {
            'Authorization': 'Bearer ' + accessToken
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        contentType: 'application/json',
        data: JSON.stringify({ refreshToken: refreshToken })
    }).success(
        function (response) {
            // Ensure new tokens are present before overwriting
            if (response && typeof response.access_token === 'string' && response.access_token.length > 0) {
                localStorage.setItem('access_token', response.access_token);
            }
            if (response && typeof response.refresh_token === 'string' && response.refresh_token.length > 0) {
                localStorage.setItem('refresh_token', response.refresh_token);
            }
        }
    );
}
