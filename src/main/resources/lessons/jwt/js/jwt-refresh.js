$(document).ready(function () {
    login('Jerry');
});

function getUserPassword() {
    var $passwordField = $('#jwt-password');

    if ($passwordField.length) {
        var pwd = $passwordField.val();
        if (typeof pwd === 'string' && pwd.length <= 128) {
            return pwd;
        }
    }

    return null;
}

function login(user) {
    var password = getUserPassword();

    if (password === null) {
        console.warn('JWT login aborted: password not provided via secure input.');
        return;
    }

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: 'application/json',
        data: JSON.stringify({ user: user, password: password })
    }).success(function (response) {
        localStorage.setItem('access_token', response['access_token']);
        localStorage.setItem('refresh_token', response['refresh_token']);
    });
}

webgoat.customjs.addBearerToken = function () {
    var headers_to_set = {};
    headers_to_set['Authorization'] = 'Bearer ' + localStorage.getItem('access_token');
    return headers_to_set;
};

function newToken() {
    localStorage.getItem('refreshToken');
    $.ajax({
        headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('access_token')
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        data: JSON.stringify({ refreshToken: localStorage.getItem('refresh_token') })
    }).success(function () {
        localStorage.setItem('access_token', apiToken);
        localStorage.setItem('refresh_token', refreshToken);
    });
}
