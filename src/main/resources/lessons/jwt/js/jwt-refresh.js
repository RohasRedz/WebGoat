$(document).ready(function () {
    login('Jerry');
});

function login(user) {
    var configuredPassword =
        (window.webgoat &&
            window.webgoat.config &&
            typeof window.webgoat.config.jwtDemoPassword === 'string' &&
            window.webgoat.config.jwtDemoPassword) ||
        document.querySelector('[data-jwt-demo-password]')?.getAttribute('data-jwt-demo-password') ||
        'CHANGE_ME_IN_CONFIGURATION';

    $.ajax({
        type: 'POST',
        url: 'JWT/refresh/login',
        contentType: 'application/json',
        data: JSON.stringify({ user: user, password: configuredPassword })
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
    var storedRefreshToken = localStorage.getItem('refresh_token');

    $.ajax({
        headers: {
            Authorization: 'Bearer ' + localStorage.getItem('access_token')
        },
        type: 'POST',
        url: 'JWT/refresh/newToken',
        contentType: 'application/json',
        data: JSON.stringify({ refreshToken: storedRefreshToken })
    }).success(function (response) {
        if (response && response.access_token && response.refresh_token) {
            localStorage.setItem('access_token', response.access_token);
            localStorage.setItem('refresh_token', response.refresh_token);
        }
    });
}
