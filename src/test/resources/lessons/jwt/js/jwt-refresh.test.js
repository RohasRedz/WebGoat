// Delta tests for jwt-refresh.js focusing on removal of hard-coded password
// and ensuring that login() uses a runtime-supplied password.

const fs = require('fs');
const path = require('path');

describe('jwt-refresh.js security delta tests', () => {
  const scriptPath = path.join(__dirname, '../../../../../../src/main/resources/lessons/jwt/js/jwt-refresh.js');
  const scriptContent = fs.readFileSync(scriptPath, 'utf8');

  test('does not contain the original hard-coded password literal', () => {
    // The original vulnerable value was "bm5nhSkxCXZkKRy4"; it must not
    // appear anywhere in the updated script.
    expect(scriptContent.includes('bm5nhSkxCXZkKRy4')).toBe(false);
  });

  test('login function uses a password parameter instead of a hard-coded literal', () => {
    // Check that the login signature accepts a password parameter
    expect(scriptContent).toMatch(/function\s+login\s*\(\s*user\s*,\s*password\s*\)/);

    // Check that the AJAX call uses the password variable and not a string literal
    expect(scriptContent).toMatch(/JSON\.stringify\(\{\s*user:\s*user,\s*password:\s*password\s*\}\)/);
  });
});
