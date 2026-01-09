// Resolved test path (from src/main/...):
// src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

// NOTE: This test assumes a Jest environment with jsdom so that `document` and AMD `define`
// resolution are available or shimmed appropriately. For pure unit testing, we directly
// require the module as CommonJS if it is bundled that way in the test setup.

// TODO: Adjust the import/require below if your build transforms AMD modules differently.
const LessonContentModel = require('../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');

describe('LessonContentModel URL regex behavior (delta tests)', () => {
  let originalLocation;

  beforeAll(() => {
    originalLocation = global.window && global.window.location ? { ...window.location } : null;
  });

  afterAll(() => {
    if (originalLocation && global.window) {
      Object.defineProperty(window, 'location', {
        value: originalLocation,
        writable: true,
      });
    }
  });

  function setDocumentUrl(url) {
    // jsdom allows overriding window.location via defineProperty
    Object.defineProperty(window, 'location', {
      value: new URL(url, 'http://localhost'),
      writable: true,
    });
    Object.defineProperty(window.document, 'URL', {
      value: url,
      writable: true,
    });
  }

  test('sets lessonUrl and pageNum correctly for typical lesson URL with page', () => {
    // Arrange
    setDocumentUrl('http://localhost/SomeLesson.lesson/3');
    const model = new LessonContentModel();

    // Act
    model.setContent('<html>content</html>', true);

    // Assert
    expect(model.get('lessonUrl')).toBe('http://localhost/SomeLesson.lesson');
    expect(model.get('pageNum')).toBe('3');
  });

  test('sets lessonUrl and default pageNum for lesson URL without page', () => {
    // Arrange
    setDocumentUrl('http://localhost/AnotherLesson.lesson');
    const model = new LessonContentModel();

    // Act
    model.setContent('<html>content</html>', true);

    // Assert
    expect(model.get('lessonUrl')).toBe('http://localhost/AnotherLesson.lesson');
    expect(model.get('pageNum')).toBe(0);
  });

  test('regex does not misbehave on complex path and still extracts page number', () => {
    // Arrange: URL with additional segments before .lesson
    setDocumentUrl('http://localhost/app/path/Lesson-Name.lesson/12');
    const model = new LessonContentModel();

    // Act
    model.setContent('<html>content</html>', true);

    // Assert
    // The tightened regex should still correctly extract the lessonUrl and pageNum
    expect(model.get('lessonUrl')).toBe('http://localhost/app/path/Lesson-Name.lesson');
    expect(model.get('pageNum')).toBe('12');
  });
});
