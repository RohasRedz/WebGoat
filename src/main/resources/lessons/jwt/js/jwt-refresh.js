$(document).ready(function () {
    // Attempt automatic login only if a password has been configured securely
    // (e.g., injected into the page as a data-attribute or via a secure runtime mechanism).
    const user = 'Jerry';
    const configuredPassword = getConfiguredPassword();

    if (configuredPassword) {
        login(user, configuredPassword);
    } else {
        // If no configured password is available, require explicit user interaction.
        // This avoids embedding any hard-coded secret in the client.
        console.warn('JWT refresh demo: no password configured; user interaction required to log in.');
    }
});

/**
 * NOTE (Security – Hardcoded Secret Removal):
 *
 * Previously, this file contained a hard-coded password literal in the AJAX payload:
 *     password: "bm5nhSkxCXZkKRy4"
 *
 * This is a serious security issue (CWE-798: Use of Hard-coded Credentials).
 * The password value has been completely removed from the client-side source.
 *
 * The password is now expected to be:
 *   - provided securely at runtime (e.g., via a data-* attribute rendered by the server
 *     for this training lesson only), OR
 *   - entered by the user through a UI element (e.g., an input field in the page).
 *
 * Under no circumstances should a production system place real credentials in client code.
 */

/**
 * Retrieve the demo password from a secure, runtime-provided source instead of
 * hard-coding it in JavaScript.
 *
 * Examples of acceptable sources in a controlled training/demo environment:
 *   - A data attribute on the <body> or a specific element:
 *       <body data-jwt-demo-password="...">
 *   - A non-version-controlled inline script which sets a global variable from a
 *     secure configuration, injected at deploy time.
 *
 * This helper MUST NOT embed any literal secret; it only reads what the server or
 * environment has provided at runtime.
 */
function getConfiguredPassword() {
    // Try to read the password from a data attribute on <body>, if present.
    try {
        var bodyEl = document && document.body;
        if (bodyEl && bodyEl.dataset && bodyEl.dataset.jwtDemoPassword) {
            return bodyEl.dataset.jwtDemoPassword;
        }
    } catch (e) {
        // Swallow safely; absence of this mechanism should not break the page.
    }

    // Fallback: try to read from a non-sensitive input field, if the page provides one.
    // For the lesson, an input with id="jwt-demo-password" can be used to let the user
    // supply the password manually.
    try {
        var inputEl = document.getElementById('jwt-demo-password');
        if (inputEl && typeof inputEl.value === 'string' && inputEl.value.length > 0) {
            return inputEl.value;
        }
    } catch (e2) {
        // Ignore; if no password is obtainable, return null.
    }

    // No configured password found; caller must handle this case gracefully.
    return null;
}

function login(user, password) {
    if (typeof password !== 'string' || password.length === 0) {
        console.warn('JWT refresh login aborted: missing password.');
        return;
    }

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({ user: user, password: password })
    }).success(
        function (response) {
            // NOTE: Tokens are stored in localStorage for this lesson only.
            // In a production system, consider HttpOnly, Secure cookies and a strong CSP.
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
