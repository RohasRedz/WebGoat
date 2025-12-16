$(document).ready(function () {
    login('Jerry');
});

function getLessonPassword() {
    /**
     * TODO(security): This lesson previously used a hard-coded password literal.
     * For production code, NEVER hard-code secrets in source files.
     *
     * Use a proper secret-management mechanism instead, such as:
     * - Environment variables injected at build/runtime
     * - A secure secret manager (Vault, KMS, etc.)
     * - Server-side configuration that is not exposed to the client
     *
     * For this training lesson, the password is obtained from a non-secret
     * configuration value on the page (if present), or falls back to a clearly
     * marked placeholder that MUST be replaced in real deployments.
     */
    var el = document.getElementById('jwt_lesson_password');
    if (el && typeof el.value === 'string' && el.value.length > 0) {
        return el.value;
    }
    // Fallback placeholder; this is intentionally not a real secret.
    return 'CHANGE_ME_LESSON_PASSWORD';
}

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({
            user: user,
            // FIX: Removed hard-coded password literal and replaced with a
            // configuration-driven helper suitable for lessons.
            password: getLessonPassword()
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
