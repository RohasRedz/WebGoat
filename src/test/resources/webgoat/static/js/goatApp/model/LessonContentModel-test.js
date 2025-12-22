const LessonContentModel = require('../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');

describe('LessonContentModel delta tests - safe URL parsing', () => {
  let model;

  beforeEach(() => {
    model = new LessonContentModel();
    if (!model.set) {
      model.set = jest.fn();
    } else {
      jest.spyOn(model, 'set');
    }
    if (!model.trigger) {
      model.trigger = jest.fn();
    } else {
      jest.spyOn(model, 'trigger');
    }
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test('setContent uses safe parsing for lessonUrl and pageNum for standard .lesson/<digits> URL', () => {
    const originalUrl = 'http://example.com/intro.lesson/12';
    delete global.document;
    global.document = { URL: originalUrl };

    const content = '<html>content</html>';

    model.setContent(content, true);

    const calls = model.set.mock.calls;

    const lessonUrlCall = calls.find(([key]) => key === 'lessonUrl');
    const pageNumCall = calls.find(([key]) => key === 'pageNum');

    expect(lessonUrlCall).toBeDefined();
    expect(lessonUrlCall[1]).toBe('http://example.com/intro.lesson');

    expect(pageNumCall).toBeDefined();
    expect(pageNumCall[1]).toBe(12);
  });

  test('setContent safely falls back to pageNum 0 for long or unexpected URLs', () => {
    const longSegment = 'a'.repeat(3000);
    const longUrl = `http://example.com/intro.lesson/${longSegment}`;
    delete global.document;
    global.document = { URL: longUrl };

    const content = '<html>content</html>';

    model.setContent(content, true);

    const pageNumCall = model.set.mock.calls.find(([key]) => key === 'pageNum');
    expect(pageNumCall).toBeDefined();
    expect(pageNumCall[1]).toBe(0);
  });
});
