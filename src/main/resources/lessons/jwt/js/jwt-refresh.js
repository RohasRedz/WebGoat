$(document).ready(function () {
    login('Jerry');
});

/**
 * FIX: Avoid hard-coded password in source code.
 * The password is now retrieved from a configuration variable that should be
 * provided securely via server-side templating or environment-backed config.
 * In this training context, we fall back to an empty string if not present to
 * avoid embedding a real secret.
 */
function getConfiguredPassword() {
    // In a real system, this should be injected server-side as a constant
    // or via a configuration endpoint that does not expose secrets in source.
    // Example (server-side templating):
    //   const password = '${WEBGOAT_JWT_DEMO_PASSWORD}';
    // Here we default to an empty string to avoid hard-coded secrets.
    var configured = (typeof WEBGOAT_JWT_DEMO_PASSWORD !== 'undefined')
        ? WEBGOAT_JWT_DEMO_PASSWORD
        : '';
    return configured;
}

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({
            user: user,
            // FIX: use configuration-based password instead of hard-coded literal
            password: getConfiguredPassword()
        })
    }).success(
        function (response) {
            // NOTE: This is a training application; in production, prefer HttpOnly cookies
            // over localStorage for access/refresh tokens.
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
        data: JSON.stringify({refreshToken: localStorage.getItem('refresh_token')})
    }).success(
        function () {
            localStorage.setItem('access_token', apiToken);
            localStorage.setItem('refresh_token', refreshToken);
        }
    );
}
