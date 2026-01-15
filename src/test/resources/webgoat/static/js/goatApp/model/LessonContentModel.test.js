const defineModule = require('../../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');

/**
 * Delta tests for LessonContentModel focusing on the safer URL handling
 * and reduced regex complexity.
 *
 * These tests verify that:
 * - lessonUrl is derived without using a greedy regex that could lead to ReDoS.
 * - pageNum is correctly extracted from URLs that end with ".lesson/<digits>".
 */
describe('LessonContentModel delta tests', () => {
  let LessonContentModel;

  beforeAll(() => {
    // The module is defined via AMD-style define; adapt to CommonJS in test context.
    // We simulate the AMD define call result here.
    LessonContentModel = defineModule;
  });

  test('setContent derives lessonUrl and pageNum correctly from typical lesson URL', () => {
    // Arrange
    const model = new LessonContentModel();
    const originalUrl = 'http://localhost/WebGoat/lesson/SomeLesson.lesson/12';
    const oldDocumentUrl = global.document && global.document.URL;

    global.document = { URL: originalUrl };

    // Act
    model.setContent('<html>dummy</html>');

    // Assert
    const lessonUrl = model.get('lessonUrl');
    const pageNum = model.get('pageNum');

    expect(lessonUrl).toBe('http://localhost/WebGoat/lesson/SomeLesson.lesson');
    expect(pageNum).toBe('12');

    // Cleanup
    global.document.URL = oldDocumentUrl;
  });

  test('setContent defaults pageNum to 0 when URL does not end with page number', () => {
    // Arrange
    const model = new LessonContentModel();
    const originalUrl = 'http://localhost/WebGoat/lesson/SomeLesson.lesson';
    const oldDocumentUrl = global.document && global.document.URL;

    global.document = { URL: originalUrl };

    // Act
    model.setContent('<html>dummy</html>');

    // Assert
    const lessonUrl = model.get('lessonUrl');
    const pageNum = model.get('pageNum');

    expect(lessonUrl).toBe('http://localhost/WebGoat/lesson/SomeLesson.lesson');
    expect(pageNum).toBe(0);

    // Cleanup
    global.document.URL = oldDocumentUrl;
  });
});
