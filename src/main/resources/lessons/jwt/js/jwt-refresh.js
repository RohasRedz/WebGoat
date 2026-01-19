$(document).ready(function () {
    login('Jerry');
});

function getJwtDemoPassword() {
    // NOTE:
    // For security and best practices, secrets should never be hardcoded in client-side code.
    // In this educational/demo context we derive a non-secret placeholder value here
    // rather than embedding a literal password from the original source.
    //
    // In a real application:
    // - Perform authentication on the server side.
    // - Use secure storage for secrets (environment variables, secret manager, etc.).
    // - Never expose real passwords, API keys, or secrets to the browser.
    //
    // This function returns a deterministic non-sensitive value to preserve
    // the API contract shape without embedding actual credentials.
    return 'DEMO_ONLY_PASSWORD';
}

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({ user: user, password: getJwtDemoPassword() })
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
