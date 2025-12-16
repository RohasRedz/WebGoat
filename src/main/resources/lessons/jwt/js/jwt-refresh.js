// src/main/resources/lessons/jwt/js/jwt-refresh.js

/**
 * IMPORTANT SECURITY NOTE:
 * ------------------------
 * The password / secret used for authentication or JWT refresh MUST NOT be hard-coded
 * into client-side JavaScript. In a real application, secrets must be:
 *   - Stored only on the server side (e.g., environment variables, secret manager)
 *   - Never exposed in browser-delivered JS
 *
 * For this lesson, we simulate obtaining a password from a non-hardcoded source (e.g.,
 * user input field or a configuration object that is NOT committed with real secrets).
 */

const webgoatJwtConfig = window.webgoatJwtConfig || {};

/**
 * Safely obtain the password without hard-coding it in source.
 * In a real app, the server should handle authentication and token issuance, not the client.
 */
function getJwtLessonPassword() {
    // Option 1 (preferred for demos): from a non-secret UI element (e.g., input field)
    const passwordInput = document.getElementById('jwt-lesson-password');
    if (passwordInput && passwordInput.value) {
        return passwordInput.value;
    }

    // Option 2: from a non-secret, non-committed config object injected at runtime.
    // NOTE: This must not contain production secrets; for training/demo only.
    if (typeof webgoatJwtConfig.defaultPassword === 'string' && webgoatJwtConfig.defaultPassword.length > 0) {
        return webgoatJwtConfig.defaultPassword;
    }

    // Fallback: empty string to avoid hard-coded secret in code.
    // The server-side should reject this if a real password is required.
    return '';
}

$(document).ready(function () {
    // For the lesson, we still trigger a login for 'Jerry', but without embedding a hard-coded password.
    login('Jerry');
});

function login(user) {
    const password = getJwtLessonPassword();

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

// Dev comment: Pass token as header as we had an issue with tokens ending up in the access_log
webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    headers_to_set['Authorization'] = 'Bearer ' + localStorage.getItem('access_token');
    return headers_to_set;
};

// Dev comment: Temporarily disabled from page we need to work out the refresh token flow but for now we can go live with the checkout page
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
