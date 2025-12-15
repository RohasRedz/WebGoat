const fs = require('fs');
const path = require('path');

// Simple delta-style test to validate that jwt-refresh.js no longer contains a hard-coded password
// and that the login function uses the password argument when constructing the AJAX payload.

describe('jwt-refresh.js security delta tests', () => {
    const scriptPath = path.join(__dirname, '../../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    const fileContent = fs.readFileSync(scriptPath, 'utf8');

    test('should not contain the original hard-coded password literal', () => {
        expect(fileContent.includes('bm5nhSkxCXZkKRy4')).toBe(false);
    });

    test('should define login function that uses the password parameter', () => {
        // Very lightweight static validation: ensure the signature and usage are present in the file.
        expect(fileContent).toMatch(/function\s+login\s*\(\s*user\s*,\s*password\s*\)/);
        expect(fileContent).toMatch(/JSON\.stringify\(\{\s*user:\s*user,\s*password:\s*password\s*\}\)/);
    });

    test('should use configurable lessonPassword instead of hard-coded secret', () => {
        expect(fileContent).toMatch(/const\s+lessonPassword\s*=\s*window\.WEBGOAT_JWT_REFRESH_PASSWORD/);
        expect(fileContent).toMatch(/login\('Jerry'\s*,\s*lessonPassword\)/);
    });
});
