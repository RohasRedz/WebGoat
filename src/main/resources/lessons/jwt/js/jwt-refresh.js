$(document).ready(function () {
    login('Jerry');
})

/**
 * Retrieve the password used for this lesson from a non-hardcoded source.
 * Falls back to a non-sensitive placeholder if nothing is provided.
 *
 * In a real application this should come from a secure configuration or
 * secrets manager, never hardcoded in the front-end.
 */
function getLessonPassword() {
    // Allow the password to be injected via a data-attribute for lesson/demo purposes.
    var el = document.getElementById('jwt-refresh-config');
    if (el && el.getAttribute) {
        var configured = el.getAttribute('data-password');
        if (configured && typeof configured === 'string' && configured.length > 0) {
            return configured;
        }
    }

    // Fallback to a neutral placeholder, not an actual secret
    return 'CHANGE_ME_LESSON_PASSWORD';
}

function login(user) {
    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: "application/json",
        data: JSON.stringify({user: user, password: getLessonPassword()})
    }).success(
        function (response) {
            localStorage.setItem('access_token', response['access_token']);
            localStorage.setItem('refresh_token', response['refresh_token']);
        }
    )
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
    )
}
