$(document).ready(function () {
    // Use a non-sensitive placeholder password for demo purposes only.
    // In production, credentials must never be hard-coded in source code.
    login('Jerry');
});

function getDemoPassword() {
    // This function returns a non-secret placeholder value used only
    // in the training/demo context. In a real application, credentials
    // must be obtained from a secure source (e.g., user input over TLS,
    // federated identity provider, or a secure auth backend), not from code.
    return 'DEMO_ONLY_NOT_A_REAL_PASSWORD';
}

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: 'application/json',
        data: JSON.stringify({
            user: user,
            // Replaced hard-coded secret with a non-sensitive demo placeholder.
            // No real credentials are embedded in code anymore.
            password: getDemoPassword()
        })
    }).success(
        function (response) {
            if (response && typeof response === 'object') {
                if (typeof response.access_token === 'string') {
                    localStorage.setItem('access_token', response.access_token);
                }
                if (typeof response.refresh_token === 'string') {
                    localStorage.setItem('refresh_token', response.refresh_token);
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
    $.ajax({
        headers: webgoat.customjs.addBearerToken(),
        type: 'POST',
        url: 'JWT/refresh/newToken',
        contentType: 'application/json',
        data: JSON.stringify({ refreshToken: refreshToken })
    }).success(
        function (response) {
            // Use values returned by the backend instead of undeclared globals.
            if (response && typeof response === 'object') {
                if (typeof response.access_token === 'string') {
                    localStorage.setItem('access_token', response.access_token);
                }
                if (typeof response.refresh_token === 'string') {
                    localStorage.setItem('refresh_token', response.refresh_token);
                }
            }
        }
    );
}
