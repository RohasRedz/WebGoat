// Test file path (derived by replacing /main/ with /test/):
// src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

// Assuming AMD modules are bundled for Jest with a compatible loader.
// We require the model module relative to the test environment setup.
// TODO: Adjust the require path if the AMD build output path differs.
const Backbone = require('backbone');
const LessonContentModel = require('../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');

describe('LessonContentModel delta tests for safer URL parsing and pageNum handling', () => {
  test('setContent sets lessonUrl to base .lesson URL and pageNum from trailing digits', () => {
    // Arrange
    const model = new LessonContentModel();
    const originalUrl = 'http://localhost:8080/WebGoat/lesson/MyLesson.lesson/12';
    const originalDocument = global.document;
    global.document = { URL: originalUrl };

    try {
      // Act
      model.setContent('<html></html>');

      // Assert
      expect(model.get('lessonUrl')).toBe(
        'http://localhost:8080/WebGoat/lesson/MyLesson.lesson'
      );
      expect(model.get('pageNum')).toBe(12);
    } finally {
      global.document = originalDocument;
    }
  });

  test('setContent defaults pageNum to 0 when URL does not end with digits', () => {
    // Arrange
    const model = new LessonContentModel();
    const originalUrl = 'http://localhost:8080/WebGoat/lesson/MyLesson.lesson';
    const originalDocument = global.document;
    global.document = { URL: originalUrl };

    try {
      // Act
      model.setContent('<html></html>');

      // Assert
      expect(model.get('lessonUrl')).toBe(
        'http://localhost:8080/WebGoat/lesson/MyLesson.lesson'
      );
      expect(model.get('pageNum')).toBe(0);
    } finally {
      global.document = originalDocument;
    }
  });
});
