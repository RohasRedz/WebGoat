const fs = require('fs');
const path = require('path');

describe('LessonContentModel.js delta tests (regex complexity remediation)', () => {
  const filePath = path.join(
    __dirname,
    '../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js'
  );

  test('setContent limits URL length before applying regex to mitigate ReDoS', () => {
    // Arrange
    const source = fs.readFileSync(filePath, 'utf8');

    // Assert that document.URL is normalized into a bounded variable first.
    expect(source).toMatch(/var\s+currentUrl\s*=\s*String\(document\.URL\s*\|\|\s*''\)/);
    expect(source).toMatch(/currentUrl\s*=\s*currentUrl\.slice\(0,\s*2048\)/);
  });

  test('setContent uses anchored, numeric page pattern instead of multiple greedy patterns', () => {
    // Arrange
    const source = fs.readFileSync(filePath, 'utf8');

    // Verify the new single anchored pattern is present.
    expect(source).toMatch(/var\s+pagePattern\s*=\s*\\.lesson\\/(\\d\{1,4})\$/);

    // Ensure the old greedy pattern isn't present anymore.
    expect(source).not.toMatch(/\/\.\*\\.lesson\\/(\\d\{1,4})\$/);

    // Check that pageNum is parsed from the match rather than using replace on the whole URL.
    expect(source).toMatch(/var\s+match\s*=\s*currentUrl\.match\(pagePattern\)/);
    expect(source).toMatch(/parseInt\(match\[1],\s*10\)/);
  });
});
