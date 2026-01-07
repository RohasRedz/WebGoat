jest.mock("axios", () => jest.fn());
const axios = require("axios");

// Load the factory from the main resources path
const jwtRefreshFactoryModulePath = "../../../../../main/resources/lessons/jwt/js/jwt-refresh.js";
require(jwtRefreshFactoryModulePath);

describe("jwtRefreshFactory - loginAsGuest delta tests", () => {
  let accessToken;
  let serviceRoutes;
  let webSession;
  let jwtAuthenticationFactory;
  let logoutFactory;
  let ngNotify;
  let factory;

  beforeEach(() => {
    axios.mockReset();

    accessToken = {
      current: jest.fn(),
      setToken: jest.fn()
    };
    serviceRoutes = {};
    webSession = {
      data: {},
      account: Promise.resolve()
    };
    jwtAuthenticationFactory = {
      getJwtHeaderName: jest.fn(() => "X-Authorization"),
      configureAxiosInterceptor: jest.fn(() => jest.fn())
    };
    logoutFactory = {
      logout: jest.fn()
    };
    ngNotify = {
      setSessionData: jest.fn(),
      notify: jest.fn()
    };

    // Note: jwtRefreshFactory is attached to window by the script
    factory = global.window.jwtRefreshFactory(
      accessToken,
      serviceRoutes,
      webSession,
      100000, // interval, not relevant for these delta tests
      jwtAuthenticationFactory,
      logoutFactory,
      ngNotify
    );
  });

  test("loginAsGuest uses configured guest credentials when provided", async () => {
    webSession.config = {
      guestUser: "configGuest",
      guestPassword: "configSecret"
    };

    axios.mockResolvedValue({
      headers: { "x-authorization": "jwt-token" },
      data: { some: "data" }
    });

    const loginPromise = factory.loginAsGuest(() => Promise.reject("onRejection called"));

    const axiosCall = await loginPromise.then(() => axios.mock.calls[0][0]);

    expect(axiosCall.url).toBe("/WebGoat/jwt/login");
    expect(axiosCall.method).toBe("POST");
    expect(axiosCall.data).toContain("username=configGuest");
    expect(axiosCall.data).toContain("password=configSecret");
  });

  test("loginAsGuest falls back to non-secret password when no config is present", async () => {
    // No webSession.config defined
    delete webSession.config;

    axios.mockResolvedValue({
      headers: { "x-authorization": "jwt-token" },
      data: { some: "data" }
    });

    const loginPromise = factory.loginAsGuest(() => Promise.reject("onRejection called"));

    const axiosCall = await loginPromise.then(() => axios.mock.calls[0][0]);

    expect(axiosCall.data).toContain("username=guest");
    // The updated code uses empty string as default password; ensure no hard-coded 'guest' password
    expect(axiosCall.data).toContain("password=");
    expect(axiosCall.data).not.toContain("password=guest");
  });
});
