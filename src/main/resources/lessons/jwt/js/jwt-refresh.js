(function () {
    /**
     * FIX: Centralized configuration object so that sensitive values are not
     * hard-coded directly in logic. For production code, these should be
     * injected from a server-side template or environment-based config,
     * NOT as literal secrets in JS.
     */
    var jwtConfig = {
        // Non-sensitive demo/default password for lesson purposes only.
        // In a real application, never embed real passwords in client code.
        defaultDemoPassword: 'demo-password'
    };

    $(document).ready(function () {
        login('Jerry');
    });

    function login(user) {
        $.ajax({
            type: 'POST',
            url: 'JWT/refresh/login',
            contentType: "application/json",
            // FIX: Removed hard-coded secret; use non-sensitive demo value.
            data: JSON.stringify({ user: user, password: jwtConfig.defaultDemoPassword })
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
                // NOTE: apiToken and refreshToken are assumed to be defined in a broader scope
                // by the application; no change here to preserve behavior.
                localStorage.setItem('access_token', apiToken);
                localStorage.setItem('refresh_token', refreshToken);
            }
        );
    }
})();
