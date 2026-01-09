// NOTE: Assumed Jest test environment and module loading via require.
// If your project uses AMD in tests as well, adapt the import mechanism accordingly.

const { JSDOM } = require('jsdom');

// We assume LessonContentModel is exposed as a CommonJS/UMD module for testing.
// If not, this require path may need adjustment based on your bundler/test setup.
const LessonContentModel = require('../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');

describe('LessonContentModel delta tests', () => {
  let dom;
  let window;
  let document;

  beforeEach(() => {
    dom = new JSDOM(`<!DOCTYPE html><p>Hello</p>`, {
      url: 'http://example.com'
    });
    window = dom.window;
    document = window.document;
    global.document = document;
  });

  afterEach(() => {
    delete global.document;
  });

  test('setContent should normalize lessonUrl to .lesson and parse pageNum when present', () => {
    // Arrange
    // Simulate a URL that includes a page number after .lesson
    const urlWithPage = 'http://example.com/SomeLesson.lesson/123';
    Object.defineProperty(global.document, 'URL', {
      value: urlWithPage,
      configurable: true
    });

    const model = new LessonContentModel();

    // Act
    model.setContent('<html>content</html>', true);

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/SomeLesson.lesson');
    expect(model.get('pageNum')).toBe('123');
  });

  test('setContent should set pageNum to 0 when no page segment is present', () => {
    // Arrange
    const urlWithoutPage = 'http://example.com/SomeLesson.lesson';
    Object.defineProperty(global.document, 'URL', {
      value: urlWithoutPage,
      configurable: true
    });

    const model = new LessonContentModel();

    // Act
    model.setContent('<html>content</html>', true);

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/SomeLesson.lesson');
    expect(model.get('pageNum')).toBe(0);
  });
});
