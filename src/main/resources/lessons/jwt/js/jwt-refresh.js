$(document).ready(function () {
    login('Jerry');
})

function login(user) {
    // NOTE:
    // The password value should be supplied securely from the server or configuration at build/runtime.
    // It MUST NOT be hard-coded in client-side JavaScript.
    const password = getJwtRefreshPassword();

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
    )
}

/**
 * Retrieve the JWT refresh password from a secure, non-hardcoded source.
 *
 * In a real deployment, this value should be injected via a secure mechanism
 * (e.g., server-side templating, configuration endpoint guarded by auth)
 * and MUST NOT be committed to source control.
 *
 * For this challenge code, we return an empty string by default to
 * avoid embedding secrets in the front end. The backend should enforce
 * proper authentication and reject invalid/empty passwords.
 */
function getJwtRefreshPassword() {
    // TODO: Replace this stub with a secure retrieval mechanism during deployment.
    return "";
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
        data: JSON.stringify({ refreshToken: localStorage.getItem('refresh_token') })
    }).success(
        function () {
            localStorage.setItem('access_token', apiToken);
            localStorage.setItem('refresh_token', refreshToken);
        }
    )
}
