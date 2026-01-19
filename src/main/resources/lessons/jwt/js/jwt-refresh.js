$(document).ready(function () {
    login('Jerry');
});

function getUserPassword() {
    /*
     * NOTE:
     * In a production deployment, this value MUST NOT be hard-coded.
     * It should be provided by a secure configuration mechanism such as:
     * - Environment variables injected at build or runtime
     * - A dedicated secrets manager (e.g., Vault, AWS Secrets Manager, Azure Key Vault)
     * - A secure configuration endpoint with appropriate authentication/authorization
     *
     * This placeholder returns an empty string to avoid embedding secrets in source control.
     * The backend lesson logic should be adjusted to align with this pattern if necessary.
     */
    return '';
}

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({
            user: user,
            // Password is intentionally not hard-coded here; retrieved via a placeholder that must be
            // wired to a secure configuration provider in a real deployment.
            password: getUserPassword()
        })
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
    );
}
