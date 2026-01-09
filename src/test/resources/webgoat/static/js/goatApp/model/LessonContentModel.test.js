// Delta tests for LessonContentModel.js focusing on the updated regex URL parsing:
// - Verifies that lessonUrl is normalized to end with ".lesson".
// - Verifies that pageNum is derived correctly from URLs with and without a trailing page number.

const _ = require('underscore');
const Backbone = require('backbone');

// We require the actual module under test. In the real project, the AMD loader would handle this.
// Here we emulate AMD by requiring the built file as a CommonJS module if available.
// TODO: Adjust require path if the test runner uses a different module resolution strategy.
const LessonContentModelFactory = require('../../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');

describe('LessonContentModel regex and URL parsing (delta tests)', () => {
  let LessonContentModel;

  beforeAll(() => {
    // The original file defines an AMD module returning HTMLContentModel.extend(...)
    // For purposes of this delta test, we assume the factory exports the extended model.
    LessonContentModel = LessonContentModelFactory;
  });

  test('setContent normalizes lessonUrl to end with .lesson and parses pageNum from trailing digits', () => {
    // Arrange
    const model = new LessonContentModel();
    const originalUrl = 'http://example.com/path/to/lesson.lesson/12';
    const originalDocument = global.document;
    global.document = { URL: originalUrl };

    try {
      // Act
      model.setContent('<html>dummy</html>', true);

      // Assert
      const lessonUrl = model.get('lessonUrl');
      const pageNum = model.get('pageNum');

      // After fix, lessonUrl should end with ".lesson"
      expect(lessonUrl).toBe('http://example.com/path/to/lesson.lesson');

      // pageNum should be parsed from the trailing "/12"
      expect(pageNum).toBe(12);
    } finally {
      // Cleanup
      global.document = originalDocument;
    }
  });

  test('setContent defaults pageNum to 0 when URL has no page number', () => {
    // Arrange
    const model = new LessonContentModel();
    const originalUrl = 'http://example.com/path/to/lesson.lesson';
    const originalDocument = global.document;
    global.document = { URL: originalUrl };

    try {
      // Act
      model.setContent('<html>dummy</html>', true);

      // Assert
      const lessonUrl = model.get('lessonUrl');
      const pageNum = model.get('pageNum');

      expect(lessonUrl).toBe('http://example.com/path/to/lesson.lesson');
      expect(pageNum).toBe(0);
    } finally {
      global.document = originalDocument;
    }
  });
});
