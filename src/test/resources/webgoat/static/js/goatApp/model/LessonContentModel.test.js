// Jest delta tests for LessonContentModel.js focusing on:
// - lessonUrl normalization without regex.
// - pageNum extraction with safe string operations and simple numeric regex.

jest.mock('backbone', () => {
  const Model = function () {};
  Model.prototype.set = jest.fn();
  Model.prototype.fetch = jest.fn(function () {
    return {
      done: (cb) => {
        cb('<html>dummy</html>');
        return this;
      },
    };
  });
  Model.prototype.trigger = jest.fn();
  Model.extend = function (props) {
    function Child() {
      Model.apply(this, arguments);
    }
    Child.prototype = Object.create(Model.prototype);
    Object.assign(Child.prototype, props);
    Child.extend = Model.extend;
    return Child;
  };
  return { Model };
});

jest.mock('goatApp/model/HTMLContentModel', () => {
  const Backbone = require('backbone');
  return Backbone.Model.extend({});
});

describe('LessonContentModel delta tests', () => {
  let LessonContentModel;

  beforeEach(() => {
    jest.resetModules();
    global.document = { URL: 'http://example.com/lesson' };
    LessonContentModel = require('../../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');
  });

  test('setContent normalizes lessonUrl by trimming at ".lesson"', () => {
    const model = new LessonContentModel();
    const setSpy = jest.spyOn(model, 'set');

    global.document.URL = 'http://example.com/my.lesson/page1?x=1';

    model.setContent('<html/>');

    const lessonUrlCall = setSpy.mock.calls.find(([key]) => key === 'lessonUrl');
    expect(lessonUrlCall).toBeDefined();
    expect(lessonUrlCall[1]).toBe('http://example.com/my.lesson');
  });

  test('setContent sets pageNum when URL ends with ".lesson/<1-4 digits>"', () => {
    const model = new LessonContentModel();
    const setSpy = jest.spyOn(model, 'set');

    global.document.URL = 'http://example.com/my.lesson/1234';

    model.setContent('<html/>');

    const pageNumCall = setSpy.mock.calls.find(([key]) => key === 'pageNum');
    expect(pageNumCall).toBeDefined();
    expect(pageNumCall[1]).toBe(1234);
  });

  test('setContent sets pageNum to 0 when URL has non-numeric suffix', () => {
    const model = new LessonContentModel();
    const setSpy = jest.spyOn(model, 'set');

    global.document.URL = 'http://example.com/my.lesson/not-a-number';

    model.setContent('<html/>');

    const pageNumCall = setSpy.mock.calls.find(([key]) => key === 'pageNum');
    expect(pageNumCall).toBeDefined();
    expect(pageNumCall[1]).toBe(0);
  });

  test('setContent sets pageNum to 0 when no ".lesson/<digits>" suffix exists', () => {
    const model = new LessonContentModel();
    const setSpy = jest.spyOn(model, 'set');

    global.document.URL = 'http://example.com/other';

    model.setContent('<html/>');

    const pageNumCall = setSpy.mock.calls.find(([key]) => key === 'pageNum');
    expect(pageNumCall).toBeDefined();
    expect(pageNumCall[1]).toBe(0);
  });
});
