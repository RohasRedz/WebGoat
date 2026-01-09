// File path: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

// NOTE: This test focuses only on the changed behavior around URL parsing and pageNum derivation
// in LessonContentModel.js to ensure the hardened regex/logic behaves correctly and safely.

const Backbone = require('backbone');
const _ = require('underscore');

// The AMD module under test: we approximate the AMD loading by requiring the produced module.
// In the actual environment this may be wired via RequireJS; here we simulate the exported model.
// TODO: Adjust path if AMD bundling exports under a different module path.
const LessonContentModel = require('../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');

describe('LessonContentModel URL parsing and pageNum derivation (delta tests)', () => {
  let originalDocumentUrl;

  beforeAll(() => {
    // Preserve original document.URL if present
    if (typeof document !== 'undefined' && document.URL) {
      originalDocumentUrl = document.URL;
    }
  });

  afterAll(() => {
    // Restore original document.URL after tests
    if (typeof document !== 'undefined' && originalDocumentUrl) {
      Object.defineProperty(document, 'URL', {
        value: originalDocumentUrl,
        configurable: true,
        writable: false
      });
    }
  });

  function createModel() {
    // Minimal Backbone model instantiation; HTMLContentModel behavior is not under test.
    return new LessonContentModel();
  }

  function setDocumentUrl(url) {
    // Redefine document.URL for testing; jsdom allows overriding this as a property.
    Object.defineProperty(document, 'URL', {
      value: url,
      configurable: true,
      writable: false
    });
  }

  test('should derive lessonUrl ending with .lesson and pageNum 0 when URL has no page number', () => {
    // Arrange
    setDocumentUrl('http://localhost/WebGoat/lesson/SomeLesson.lesson?param=value');
    const model = createModel();

    // Act
    model.setContent('<html></html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://localhost/WebGoat/lesson/SomeLesson.lesson');
    expect(model.get('pageNum')).toBe(0);
  });

  test('should correctly extract numeric pageNum from URL with .lesson/<page>', () => {
    // Arrange
    setDocumentUrl('http://localhost/WebGoat/lesson/SomeLesson.lesson/42');
    const model = createModel();

    // Act
    model.setContent('<html></html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://localhost/WebGoat/lesson/SomeLesson.lesson');
    expect(model.get('pageNum')).toBe('42');
  });

  test('should fall back to pageNum 0 when URL does not contain .lesson segment', () => {
    // Arrange
    setDocumentUrl('http://localhost/WebGoat/other/SomeOtherPage');
    const model = createModel();

    // Act
    model.setContent('<html></html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://localhost/WebGoat/other/SomeOtherPage');
    expect(model.get('pageNum')).toBe(0);
  });

  test('should handle long or unusual URLs without throwing (stress previous regex behavior)', () => {
    // Arrange
    const longPath = 'a'.repeat(5000);
    setDocumentUrl(`http://localhost/WebGoat/${longPath}/SomeLesson.lesson/${longPath}`);
    const model = createModel();

    // Act & Assert: the call should succeed and set some consistent values
    expect(() => model.setContent('<html></html>')).not.toThrow();

    // It should still identify the .lesson segment and set pageNum based on the trailing digits if any
    expect(model.get('lessonUrl')).toContain('.lesson');
    // trailing part has no pure numeric page, so pageNum should be 0
    expect(model.get('pageNum')).toBe(0);
  });
});
