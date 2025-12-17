// Suggested file path: src/main/resources/webgoat/static/js/goatApp/model/__tests__/LessonContentModel.test.js
// NOTE: Package/module path is inferred from the fixed file location in the workflow.
// TODO: Adjust relative require path below if the project structure differs.

const Backbone = require('backbone');
const _ = require('underscore');

// We need to load the AMD-style module. In many WebGoat builds this is browser/RequireJS-only,
// so in pure Node/Jest we approximate by requiring the compiled/bundled version.
// If the project exposes LessonContentModel via a different path, update the require accordingly.
const LessonContentModel = require('../LessonContentModel.js'); // TODO: Adjust if AMD build requires different import

describe('LessonContentModel delta tests for URL parsing', () => {
  let originalLocation;
  let originalDocumentUrl;
  let model;

  beforeEach(() => {
    // Save originals so we can restore after each test
    originalLocation = global.window && global.window.location
      ? { href: global.window.location.href }
      : undefined;
    originalDocumentUrl = global.document && global.document.URL;

    // Provide minimal window/document shims for the code under test
    if (!global.window) {
      global.window = {};
    }
    if (!global.window.location) {
      global.window.location = { href: '' };
    }
    if (!global.document) {
      global.document = {};
    }

    // Create a fresh instance of the model
    model = new LessonContentModel();
  });

  afterEach(() => {
    // Restore window.location.href
    if (originalLocation && global.window && global.window.location) {
      global.window.location.href = originalLocation.href;
    }
    // Restore document.URL
    if (typeof originalDocumentUrl !== 'undefined') {
      global.document.URL = originalDocumentUrl;
    }
  });

  function setUrl(url) {
    // Prefer window.location.href as in the fixed code; fall back to document.URL
    global.window.location.href = url;
    global.document.URL = url;
  }

  test('setContent: parses base .lesson URL without page number', () => {
    // Arrange
    setUrl('http://example.com/webgoat/Intro.lesson');

    // Act
    model.setContent('<html>content</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/webgoat/Intro.lesson');
    expect(model.get('pageNum')).toBe(0);
  });

  test('setContent: parses .lesson/<page> URL with numeric page number', () => {
    // Arrange
    setUrl('http://example.com/webgoat/Intro.lesson/3');

    // Act
    model.setContent('<html>content</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/webgoat/Intro.lesson');
    expect(model.get('pageNum')).toBe(3);
  });

  test('setContent: parses .lesson URL with query and strips it for lessonUrl', () => {
    // Arrange
    setUrl('http://example.com/webgoat/Intro.lesson?page=2&foo=bar');

    // Act
    model.setContent('<html>content</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/webgoat/Intro.lesson');
    expect(model.get('pageNum')).toBe(0);
  });

  test('setContent: parses .lesson/<page> URL with hash and normalizes lessonUrl', () => {
    // Arrange
    setUrl('http://example.com/webgoat/Intro.lesson/12#section1');

    // Act
    model.setContent('<html>content</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/webgoat/Intro.lesson');
    expect(model.get('pageNum')).toBe(12);
  });

  test('setContent: returns full URL unchanged and pageNum 0 for URLs without .lesson', () => {
    // Arrange
    setUrl('http://example.com/webgoat/dashboard');

    // Act
    model.setContent('<html>content</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/webgoat/dashboard');
    expect(model.get('pageNum')).toBe(0);
  });

  test('setContent: handles malformed trailing segment that is non-numeric', () => {
    // Arrange: page segment is non-numeric; should fall back to 0
    setUrl('http://example.com/webgoat/Intro.lesson/xyz');

    // Act
    model.setContent('<html>content</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/webgoat/Intro.lesson/xyz');
    expect(model.get('pageNum')).toBe(0);
  });

  test('setContent: triggers content:loaded event after parsing URL', () => {
    // Arrange
    setUrl('http://example.com/webgoat/Intro.lesson/5');
    const listener = jest.fn();
    model.on('content:loaded', listener);

    // Act
    model.setContent('<html>content</html>', true);

    // Assert
    expect(listener).toHaveBeenCalledTimes(1);
    const args = listener.mock.calls[0];
    expect(args[0]).toBe(model);
    expect(args[1]).toBe(true);
  });

  test('URL parsing does not rely on old inefficient patterns (behavior-based)', () => {
    // This is a behavior-focused assertion: extremely long prefix should still be
    // handled in a reasonable way and not alter the semantic result.
    // We cannot directly assert on the regex text, so we use a long URL and ensure
    // the method still returns the correct lessonUrl and pageNum.
    const longPrefix = 'a'.repeat(5000);
    const url = `http://example.com/${longPrefix}/Intro.lesson/7`;

    setUrl(url);

    // Act
    model.setContent('<html>content</html>');

    // Assert
    // Even with a long prefix, we still correctly detect the lesson and page.
    expect(model.get('lessonUrl')).toBe(`http://example.com/${longPrefix}/Intro.lesson`);
    expect(model.get('pageNum')).toBe(7);
  });
});
