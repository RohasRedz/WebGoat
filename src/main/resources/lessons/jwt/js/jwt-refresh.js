$(document).ready(function () {
    login('Jerry');
});

function login(user) {
    var requestBody = {
        user: user,
        // Password is intentionally omitted here to avoid hardcoding secrets in client-side code.
        // In a real deployment, credentials or tokens should be negotiated through a secure flow
        // (e.g., user-entered password over HTTPS or a backend-issued one-time token).
        password: ''
    };

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify(requestBody)
    }).success(
        function (response) {
            // Store tokens in memory where possible; localStorage is used here
            // for compatibility with existing WebGoat flows but is not recommended
            // for production due to XSS exposure risks.
            if (response && typeof response === 'object') {
                if (typeof response['access_token'] === 'string') {
                    localStorage.setItem('access_token', response['access_token']);
                }
                if (typeof response['refresh_token'] === 'string') {
                    localStorage.setItem('refresh_token', response['refresh_token']);
                }
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
    var refreshToken = localStorage.getItem('refresh_token');
    if (!refreshToken) {
        return;
    }
    $.ajax({
        headers: webgoat.customjs.addBearerToken(),
        type: 'POST',
        url: 'JWT/refresh/newToken',
        contentType: 'application/json',
        data: JSON.stringify({ refreshToken: refreshToken })
    }).success(
        function (response) {
            if (response && typeof response === 'object') {
                if (typeof response['access_token'] === 'string') {
                    localStorage.setItem('access_token', response['access_token']);
                }
                if (typeof response['refresh_token'] === 'string') {
                    localStorage.setItem('refresh_token', response['refresh_token']);
                }
            }
        }
    );
}
