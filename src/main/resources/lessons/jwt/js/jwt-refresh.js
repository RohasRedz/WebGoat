$(document).ready(function () {
    login('Jerry');
});

function getJwtRefreshPassword() {
    // Retrieve the JWT refresh password from a secure configuration source.
    // This implementation deliberately avoids hard-coding secrets.
    // In production, this should be provided via a secure mechanism (e.g., injected
    // configuration, environment variable, or a secrets manager) on the server side,
    // then safely passed to the client only if absolutely necessary for the lesson.
    //
    // For this client-side code, we use a placeholder to indicate that the actual
    // secret must not live in source control.
    if (typeof window !== 'undefined' && window.webgoat && window.webgoat.config && window.webgoat.config.jwtRefreshPassword) {
        return window.webgoat.config.jwtRefreshPassword;
    }

    // Fallback placeholder – NOT a real secret. Must be replaced at runtime by the
    // platform or test harness and must not represent a production credential.
    return '***JWT_REFRESH_PASSWORD_CONFIGURED_EXTERNALLY***';
}

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({
            user: user,
            // Password/secret is now retrieved from a configurable source instead
            // of being hard-coded in the source file.
            password: getJwtRefreshPassword()
        })
    }).success(
        function (response) {
            // Store tokens in localStorage as before (lesson behavior),
            // but avoid logging or exposing them anywhere else.
            localStorage.setItem('access_token', response['access_token']);
            localStorage.setItem('refresh_token', response['refresh_token']);
        }
    );
}

// Dev note: Pass token as header to avoid tokens ending up in server access logs via query string.
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    var accessToken = localStorage.getItem('access_token');

    // Do not log tokens; just attach them to the Authorization header.
    headers_to_set['Authorization'] = 'Bearer ' + accessToken;
    return headers_to_set;
};

// Dev note: Refresh token flow retained for lesson purposes, but ensure that
// tokens are not logged or exposed beyond what the lesson requires.
function newToken() {
    var refreshToken = localStorage.getItem('refresh_token');
    $.ajax({
        headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('access_token')
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        data: JSON.stringify({ refreshToken: refreshToken })
    }).success(
        function (response) {
            // Use the tokens returned from the server response instead of undefined variables.
            if (response && response.access_token) {
                localStorage.setItem('access_token', response.access_token);
            }
            if (response && response.refresh_token) {
                localStorage.setItem('refresh_token', response.refresh_token);
            }
        }
    );
}
