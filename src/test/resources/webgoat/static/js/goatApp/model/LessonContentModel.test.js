const LessonContentModel = require('../../../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel');

describe('LessonContentModel URL parsing', () => {
  let originalDocument;

  beforeAll(() => {
    originalDocument = global.document;
  });

  afterAll(() => {
    global.document = originalDocument;
  });

  function createModelWithUrl(url) {
    global.document = { URL: url };
    return new LessonContentModel();
  }

  test('lessonUrl is truncated at the first .lesson', () => {
    const model = createModelWithUrl('http://example.com/path/to/file.lesson/extra/path');
    model.setContent('dummy');
    expect(model.get('lessonUrl')).toBe('http://example.com/path/to/file.lesson');
  });

  test('lessonUrl is unchanged when .lesson is not present', () => {
    const url = 'http://example.com/path/to/file.html';
    const model = createModelWithUrl(url);
    model.setContent('dummy');
    expect(model.get('lessonUrl')).toBe(url);
  });

  test('pageNum is set to trailing 1-4 digits after .lesson/', () => {
    const model = createModelWithUrl('http://example.com/path/to/file.lesson/123');
    model.setContent('dummy');
    expect(model.get('pageNum')).toBe('123');
  });

  test('pageNum is set to 0 when there is no .lesson/<digits> suffix', () => {
    const model = createModelWithUrl('http://example.com/path/to/file.html');
    model.setContent('dummy');
    expect(model.get('pageNum')).toBe(0);
  });
});
