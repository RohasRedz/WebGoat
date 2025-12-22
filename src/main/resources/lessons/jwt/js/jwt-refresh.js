$(document).ready(function () {
    login('Jerry');
});

function login(user) {
    var password = (typeof webgoat !== 'undefined' &&
                    webgoat.customjs &&
                    typeof webgoat.customjs.getJwtRefreshPassword === 'function')
        ? webgoat.customjs.getJwtRefreshPassword()
        : null;

    if (!password) {
        console.error('JWT refresh password is not configured securely.');
        return;
    }

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
    );
}

webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    headers_to_set['Authorization'] = 'Bearer ' + localStorage.getItem('access_token');
    return headers_to_set;
};

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
            if (response && response['access_token']) {
                localStorage.setItem('access_token', response['access_token']);
            }
            if (response && response['refresh_token']) {
                localStorage.setItem('refresh_token', response['refresh_token']);
            }
        }
    );
}
