// Delta Jest tests for jwt-refresh.js focusing only on the secret handling changes:
// - SECRET must come from process.env.JWT_SECRET
// - Module throws on require if JWT_SECRET is missing.

const path = require('path');

describe('jwt-refresh SECRET configuration (delta test)', () => {
    const modulePath = path.resolve(__dirname, '../../../../main/resources/lessons/jwt/js/jwt-refresh.js');

    afterEach(() => {
        jest.resetModules();
        delete process.env.JWT_SECRET;
    });

    test('module throws at require-time when JWT_SECRET is not defined', () => {
        delete process.env.JWT_SECRET;

        expect(() => {
            require(modulePath);
        }).toThrow(/JWT secret is not configured/i);
    });

    test('module requires successfully when JWT_SECRET is defined', () => {
        process.env.JWT_SECRET = 'test_secret_value';

        const router = require(modulePath);

        expect(router).toBeDefined();
        expect(typeof router).toBe('function');
    });
});
