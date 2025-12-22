// File path: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js
// Jest delta tests for LessonContentModel.js focusing on safer regex behavior around document.URL.
const $ = require('jquery'); // if actually needed by HTMLContentModel; otherwise can be mocked
// TODO: Adjust path below to match the actual module resolution used in the project.
jest.mock('goatApp/model/HTMLContentModel', () => {
  const Backbone = require('backbone');
  return Backbone.Model.extend({});
});

const Backbone = require('backbone');

// NOTE: We require the actual module under test after mocks are set up.
const LessonContentModelFactory = () =>
  require('../../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');

describe('LessonContentModel - delta tests for URL regex handling', () => {
  let LessonContentModel;

  beforeEach(() => {
    // Clear module cache to re-evaluate when needed
    jest.resetModules();
    LessonContentModel = LessonContentModelFactory();
  });

  test('setContent should derive lessonUrl by stripping page suffix using safe regex', () => {
    // Arrange
    const model = new LessonContentModel();
    const originalUrl = 'http://example.com/lesson1.lesson/12';
    delete global.document;
    global.document = { URL: originalUrl };

    // Act
    model.setContent('<html>content</html>');

    // Assert
    // Expected behavior: remove the "/12" page component, leaving base ".lesson" URL
    expect(model.get('lessonUrl')).toBe('http://example.com/lesson1.lesson');
  });

  test('setContent should set pageNum from trailing numeric segment or 0 when absent', () => {
    const modelWithPage = new LessonContentModel();
    const urlWithPage = 'http://example.com/lesson1.lesson/99';
    global.document = { URL: urlWithPage };

    // Act
    modelWithPage.setContent('<html>content</html>');

    // Assert
    expect(modelWithPage.get('pageNum')).toBe('99');

    const modelWithoutPage = new LessonContentModel();
    const urlWithoutPage = 'http://example.com/lesson1.lesson';
    global.document = { URL: urlWithoutPage };

    // Act
    modelWithoutPage.setContent('<html>content</html>');

    // Assert
    expect(modelWithoutPage.get('pageNum')).toBe(0);
  });

  test('setContent should handle long URLs without catastrophic backtracking', () => {
    // This test simulates that the regex can process a long URL quickly and deterministically.
    const model = new LessonContentModel();
    const repeated = 'a'.repeat(10000);
    const longUrl = `http://example.com/${repeated}.lesson/1234`;
    global.document = { URL: longUrl };

    const start = Date.now();
    model.setContent('<html>content</html>');
    const durationMs = Date.now() - start;

    // Assert: execution must remain fast, indicating safe regex usage.
    expect(durationMs).toBeLessThan(200); // generous upper bound for unit environment
    expect(model.get('lessonUrl')).toBe(
      `http://example.com/${repeated}.lesson`
    );
    expect(model.get('pageNum')).toBe('1234');
  });
});
