const $ = require('jquery');
const _ = require('underscore');
const Backbone = require('backbone');

// Minimal HTMLContentModel stub for testing; the real module may provide more behavior.
const HTMLContentModel = Backbone.Model.extend({});

// Simulate the AMD factory from LessonContentModel.js
function createLessonContentModel() {
  const factory = require('../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js'); // TODO: adjust path if needed
  // If the module directly returns the extended model, just return it.
  return factory($, _, Backbone, HTMLContentModel);
}

describe('LessonContentModel (delta tests for URL parsing)', () => {
  let LessonContentModel;

  beforeEach(() => {
    // JSDOM provides document and window; ensure they exist
    global.document = { URL: 'http://example.com/start.lesson' };
    LessonContentModel = createLessonContentModel();
  });

  test('setContent normalizes lessonUrl by stripping trailing /<page> and preserving .lesson base', () => {
    // Arrange
    const model = new LessonContentModel();
    // URL contains a numeric page suffix, which should be stripped from lessonUrl
    document.URL = 'http://example.com/lesson/attack.lesson/3';

    // Act
    model.setContent('<html>content</html>');

    // Assert
    const lessonUrl = model.get('lessonUrl');
    expect(lessonUrl).toBe('http://example.com/lesson/attack.lesson');

    // Ensure content was set as well to verify method executed
    expect(model.get('content')).toBe('<html>content</html>');
  });

  test('setContent sets pageNum to numeric suffix when URL ends with digits', () => {
    // Arrange
    const model = new LessonContentModel();
    document.URL = 'http://example.com/lesson/attack.lesson/42';

    // Act
    model.setContent('<html>content</html>');

    // Assert
    expect(model.get('pageNum')).toBe(42);
  });

  test('setContent sets pageNum to 0 when URL has no numeric tail', () => {
    // Arrange
    const model = new LessonContentModel();
    document.URL = 'http://example.com/lesson/attack.lesson';

    // Act
    model.setContent('<html>content</html>');

    // Assert
    expect(model.get('pageNum')).toBe(0);
  });
});
