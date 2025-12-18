const _ = require('underscore');
const Backbone = require('backbone');

const LessonContentModel = require('../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');

describe('LessonContentModel delta tests for URL parsing', () => {
  let model;

  beforeEach(() => {
    model = new LessonContentModel();
  });

  test('setContent computes lessonUrl and pageNum when URL contains page number', () => {
    const originalUrl = 'http://localhost:8080/WebGoat/start.lesson/12';
    global.document = { URL: originalUrl };

    const contentLoadedSpy = jest.fn();
    model.on('content:loaded', contentLoadedSpy);

    model.setContent('<html>content</html>', true);

    expect(model.get('lessonUrl')).toBe('http://localhost:8080/WebGoat/start.lesson');
    expect(model.get('pageNum')).toBe('12');
    expect(contentLoadedSpy).toHaveBeenCalledWith(model, true);
  });

  test('setContent sets pageNum to 0 when URL does not contain page number', () => {
    const originalUrl = 'http://localhost:8080/WebGoat/start.lesson';
    global.document = { URL: originalUrl };

    model.setContent('<html>content</html>', false);

    expect(model.get('lessonUrl')).toBe('http://localhost:8080/WebGoat/start.lesson');
    expect(model.get('pageNum')).toBe(0);
  });

  test('setContent handles more complex URL without catastrophic regex behavior', () => {
    const originalUrl =
      'http://localhost:8080/WebGoat/start.lesson/1234?param=1&another=2#fragment';
    global.document = { URL: originalUrl };

    model.setContent('<html>content</html>', true);

    expect(model.get('lessonUrl')).toBe('http://localhost:8080/WebGoat/start.lesson');
    expect(model.get('pageNum')).toBe('1234');
  });
});
