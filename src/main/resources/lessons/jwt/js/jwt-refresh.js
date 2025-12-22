$(document).ready(function () {
    // Use a non-hardcoded password source to avoid embedding secrets in code.
    // In this training context, fall back to a safe placeholder if nothing is configured.
    var effectivePassword = getLoginPassword();
    login('Jerry', effectivePassword);
});

/**
 * Returns the password to be used for login without hardcoding it in source.
 * Priority:
 * 1. window.WEBGOAT_JWT_PASSWORD (if set by the page or environment)
 * 2. Value from a hidden/input field with id="jwt-password" (if present in DOM)
 * 3. A non-secret placeholder string for demo-only flows
 *
 * This avoids committing real credentials into the codebase while keeping behavior functional.
 */
function getLoginPassword() {
    // 1) Allow a configurable value via a global variable (set server-side or via config script)
    if (typeof window !== 'undefined' && typeof window.WEBGOAT_JWT_PASSWORD === 'string' && window.WEBGOAT_JWT_PASSWORD.length > 0) {
        return window.WEBGOAT_JWT_PASSWORD;
    }

    // 2) Fallback: read from a DOM element if present so that any real secret
    // can be injected dynamically rather than hardcoded in JS.
    if (typeof document !== 'undefined') {
        var passwordInput = document.getElementById('jwt-password');
        if (passwordInput && typeof passwordInput.value === 'string' && passwordInput.value.length > 0) {
            return passwordInput.value;
        }
    }

    // 3) Last-resort placeholder for training/demo; not a real credential.
    return 'CHANGE_ME_IN_CONFIG';
}

function login(user, password) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: 'application/json',
        data: JSON.stringify({ user: user, password: password })
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
        data: JSON.stringify({ refreshToken: localStorage.getItem('refresh_token') })
    }).success(
        function () {
            localStorage.setItem('access_token', apiToken);
            localStorage.setItem('refresh_token', refreshToken);
        }
    );
}
