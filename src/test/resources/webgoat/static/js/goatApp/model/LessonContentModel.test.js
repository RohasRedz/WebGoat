// NOTE: This is a delta unit test focused on the updated URL parsing logic in LessonContentModel.
// It is intentionally minimal and scoped only to the security-related behavior change.

const Backbone = require('backbone');

// Minimal HTMLContentModel stub to satisfy the dependency chain.
// In the real test environment, require the actual 'goatApp/model/HTMLContentModel' instead.
class HTMLContentModel extends Backbone.Model {}

describe('LessonContentModel URL parsing (delta tests)', () => {
  let LessonContentModel;
  let originalDocument;

  beforeAll(() => {
    // Shim AMD-style define to capture the factory return value.
    global.define = function (deps, factory) {
      LessonContentModel = factory(
        require('jquery'),
        require('underscore'),
        Backbone,
        HTMLContentModel
      );
    };
    // Load the updated module (this will invoke our global.define shim).
    require('../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');
    delete global.define;
  });

  beforeEach(() => {
    originalDocument = global.document;
    global.document = { URL: '' };
  });

  afterEach(() => {
    global.document = originalDocument;
  });

  test('setContent extracts pageNum from .lesson/<digits> URL using safe parsing', () => {
    // Arrange
    const model = new LessonContentModel();
    global.document.URL = 'http://example.com/SomeLesson.lesson/12';

    // Act
    model.setContent('<html>content</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/SomeLesson.lesson');
    expect(model.get('pageNum')).toBe(12);
  });

  test('setContent defaults pageNum to 0 when URL does not match expected pattern', () => {
    // Arrange
    const model = new LessonContentModel();
    global.document.URL = 'http://example.com/SomeLesson.lesson/invalid';

    // Act
    model.setContent('<html>content</html>');

    // Assert
    expect(model.get('pageNum')).toBe(0);
  });
});
