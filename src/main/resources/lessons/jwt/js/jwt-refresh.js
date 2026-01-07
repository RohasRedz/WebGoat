"use strict";

var errorCodes = {
    GENERIC: 1,
    PROFILE_DOES_NOT_EXIST: 2,
    USERNAME_DOES_NOT_EXIST: 3,
    UNKNOWN_RENEW_ERROR: 4
}

window.jwtRefreshFactory = function (accessToken, serviceRoutes, webSession, interval, jwtAuthenticationFactory, logoutFactory, ngNotify) {
    var jwtHeaderName = jwtAuthenticationFactory.getJwtHeaderName();

    function getExpiryFromToken(encodedToken) {
        var token = JSON.parse(atob(encodedToken.split(".")[1]));
        return token.exp * 1000;
    }

    function setPermissions(obj) {
        var disabled = (obj.webgoat_principal && obj.webgoat_principal.webgoat_user && obj.webgoat_principal.webgoat_user.lessons.length == 0) ||
            webSession.data.username === "guest";
        webSession.data.disabled = disabled;
        webSession.data.messages = [];
        
        if (disabled) {
            if (webSession.data.username === "guest") {
                webSession.data.messages.push("As guest you are not able to track your progress.");
            }
            webSession.data.messages.push("Some challenges might be disabled. Please login if you want to track your progress and want all challenges available.");
        }
    }

    function asUser(operator) {
        return function(payload) {
            return webSession.account
            .then(function() {}, function() {
                return axios({
                    url: serviceRoutes.profile,
                    method: 'GET',
                    beforeSend: function(request) {
                        request.setRequestHeader(jwtHeaderName, payload.jwt);
                    }
                })
                .then(function(response) {
                    var data = response.data;
                    webSession.data.username = data.webgoat_principal.username;
                    webSession.data.apiAuthenticated = 'true';
                    setPermissions(data);
                    return {jwt: payload.jwt, expiry: payload.expiry};
                })
                .catch(function(response) {
                    webSession.data.apiAuthenticated = 'false';
                    if (response.status === 404 && response.data && response.data.code === errorCodes.PROFILE_DOES_NOT_EXIST) {
                        //$location.path('/user/create');
                    }
                    return Promise.reject(response);
                });
            })
            .then(operator);
        };
    }

    function loadServices(http) {
        return function(payload) {
            serviceRoutes.profile = payload.jwtData.links["/profile"].href;
            serviceRoutes.changePassword = payload.jwtData.links["/profile/password"].href;
            serviceRoutes.feedback = payload.jwtData.links["/feedback"].href;
            serviceRoutes.resetLesson = payload.jwtData.links["/lessons"].href;
            return payload.jwt;
        }
    }

    function login(username, password) {
        return axios({
            url: '/WebGoat/jwt/login',
            method: 'POST',
            data: "username="+username+"&password="+password,
            headers: {'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'}
        })
        .then(function(response) {
            var jwt = response.headers['x-authorization'];
            var expiry = getExpiryFromToken(jwt);
            const jwtData = response.data;
            setInterval(renew, interval);
            return {jwt, expiry, jwtData};
        });
    }

    function notifyUser(operator) {
        var notified = false;
        return function(data) {
            if (!notified) {
                try {
                    operator(data);
                    notified = true;
                } catch (error) {
                    console.error(error);
                }
            }            
            return data;
        }
    }

    var renewers = [];

    function addRenewer(renewer) {
        renewers.push(renewer);
        return renewer;
    }

    function shouldRenew(data) {
        return data.expiry < (new Date()).getTime() + 100000;
    }

    function onlyAuthenticated(data) {
        return data;
    }

    function renew() {
        renewJwt(onlyAuthenticated, function(rejection) {
            logoutFactory.logout();
        });
    }

    function renewJwt(shouldRenewFn, onRejection) {
        if (!accessToken.current()) {
            return Promise.reject('No current JWT, skipping renew, user might not be logged in');
        }
        return axios({
            url: '/WebGoat/jwt/refresh',
            method: 'POST',
            data: {},
            beforeSend: function(request) {
                request.setRequestHeader(jwtHeaderName, accessToken.current());
            }
        })
        .then(function (response) {
            const refreshedJwt = response.headers['x-authorization'];
            const expiry = getExpiryFromToken(refreshedJwt);
            const jwtData = response.data;
            for (var i=0; i<renewers.length; i+=1) {
                try {
                    renewers[i](refreshedJwt, expiry, jwtData);
                } catch (error) {
                    console.log(error);
                }
            }
            return {jwt: refreshedJwt, expiry, jwtData};
        })
        .catch(function (response) {
            if (onRejection) {
                onRejection(response);
            } else {
                return Promise.reject(response);
            }
        });
    }

    function renewJwtEvery(shouldRenewFn, onRejection, delayInMs) {
        setInterval(function() {
            renewJwt(shouldRenewFn, onRejection);
        }, delayInMs);
    }

    function loginAsUser(username, word, onRejection) {
        return login(username, word)
        .then(addRenewer(accessToken.setToken))
        .then(addRenewer(jwtAuthenticationFactory.configureAxiosInterceptor))
        .then(notifyUser(ngNotify.setSessionData))
        .then(notifyUser(ngNotify.notify))
        .then(function(data) { webSession.data.apiAuthenticated = 'true'; return data; })
        .catch(function(response) {
            return onRejection(response);
        });
    }

    function resolveGuestCredentials() {
        // Prefer configuration-based guest credentials; fall back to non-secret defaults
        var guestUser = (webSession.config && webSession.config.guestUser) || 'guest';
        var guestPassword = (webSession.config && webSession.config.guestPassword) || '';
        return { username: guestUser, password: guestPassword };
    }

    function loginAsGuest(onRejection) {
        var creds = resolveGuestCredentials();
        return login(creds.username, creds.password)
        .then(addRenewer(accessToken.setToken))
        .then(addRenewer(jwtAuthenticationFactory.configureAxiosInterceptor))
        .then(notifyUser(ngNotify.setSessionData))
        .then(notifyUser(ngNotify.notify))
        .then(function(data) { webSession.data.apiAuthenticated = 'true'; return data; })
        .catch(function(response) {
            return onRejection(response);
        });
    }

    return {
        'loginAsUser': loginAsUser,
        'loginAsGuest': loginAsGuest,
        'loadServices': loadServices,
        'asUser': asUser,
        'renewJwtEvery': renewJwtEvery,
        'renew': renew
    };
}
