const LessonContentModelModulePath = '../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel';

let ajaxMock;

jest.mock('backbone', () => {
  const actual = jest.requireActual('backbone');
  class MockModel extends actual.Model {
    fetch(options) {
      return {
        done: (cb) => {
          cb('<html>content</html>');
          return this;
        },
      };
    }
  }
  return {
    ...actual,
    Model: MockModel,
  };
});

jest.mock('underscore', () => {
  const actual = jest.requireActual('underscore');
  return {
    ...actual,
    escape: jest.fn((s) => `[escaped]${s}`),
  };
});

jest.mock('jquery', () => ({}));

const LessonContentModel = require(LessonContentModelModulePath);

describe('LessonContentModel (delta tests)', () => {
  beforeEach(() => {
    global.document = {
      URL: 'http://example.com/lesson/1234',
    };
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  test('loadData should defensively encode and escape options.name into urlRoot', () => {
    const model = new LessonContentModel();
    const options = { name: 'My Lesson' };

    model.loadData(options);

    const _ = require('underscore');
    expect(_.escape).toHaveBeenCalledTimes(1);
    expect(_.escape).toHaveBeenCalledWith(encodeURIComponent(options.name));

    expect(model.urlRoot).toBe(`[escaped]${encodeURIComponent(options.name)}.lesson`);
  });

  test('setContent should derive lessonUrl and pageNum using bounded regexes', () => {
    const model = new LessonContentModel();

    const setSpy = jest.spyOn(model, 'set');
    const triggerSpy = jest.spyOn(model, 'trigger');

    model.setContent('<html>content</html>', true);

    expect(setSpy).toHaveBeenCalledWith('content', '<html>content</html>');

    const lessonUrlCall = setSpy.mock.calls.find((c) => c[0] === 'lessonUrl');
    expect(lessonUrlCall).toBeDefined();
    expect(lessonUrlCall[1]).toBe('http://example.com/lesson.lesson');

    const pageNumCall = setSpy.mock.calls.find((c) => c[0] === 'pageNum');
    expect(pageNumCall).toBeDefined();
    expect(pageNumCall[1]).toBe('1234');

    expect(triggerSpy).toHaveBeenCalledWith('content:loaded', model, true);
  });

  test('setContent should fall back gracefully when URL does not match expected pattern', () => {
    const model = new LessonContentModel();
    global.document.URL = 'http://example.com/other';

    const setSpy = jest.spyOn(model, 'set');

    model.setContent('<html>content</html>', false);

    const lessonUrlCall = setSpy.mock.calls.find((c) => c[0] === 'lessonUrl');
    expect(lessonUrlCall).toBeDefined();
    expect(lessonUrlCall[1]).toBe('http://example.com/other');

    const pageNumCall = setSpy.mock.calls.find((c) => c[0] === 'pageNum');
    expect(pageNumCall).toBeDefined();
    expect(pageNumCall[1]).toBe(0);
  });
});
