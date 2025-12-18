const fs = require('fs');
const path = require('path');

describe('jwt-refresh.js delta tests (hard-coded password removal)', () => {
  test('login uses non-secret placeholder instead of hard-coded password literal', () => {
    // Arrange
    const filePath = path.join(__dirname, '../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    const source = fs.readFileSync(filePath, 'utf8');

    // Assert that the original hard-coded password value is no longer present.
    expect(source).not.toMatch(/bm5nhSkxCXZkKRy4/);

    // Assert that a clearly non-secret placeholder is used instead.
    expect(source).toMatch(/PASSWORD_PLACEHOLDER/);
    expect(source).toMatch(/<<REPLACE_WITH_SECURE_INPUT_OR_SERVER_SIDE_AUTH>>/);

    // Ensure the login function still sends a password field in the JSON payload.
    expect(source).toMatch(/data:\s*JSON\.stringify\(\s*{\s*user:\s*user,\s*[\s\S]*password:/);
  });

  test('login performs AJAX POST to JWT\\/refresh\\/login with JSON content type', () => {
    // Arrange
    const filePath = path.join(__dirname, '../../../main/resources/lessons/jwt/js/jwt-refresh.js');
    const source = fs.readFileSync(filePath, 'utf8');

    // This guards against accidental regression of the flow around the removed secret.
    expect(source).toMatch(/type:\s*'POST'/);
    expect(source).toMatch(/url:\s*'JWT\/refresh\/login'/);
    expect(source).toMatch(/contentType:\s*['"]application\/json['"]/);
  });
});
