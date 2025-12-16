jest.mock('backbone', () => {
  const Backbone = {
    Model: function () {},
  };
  Backbone.Model.prototype.fetch = jest.fn(function (options) {
    return {
      done: (cb) => {
        cb('<html>dummy</html>');
        return { done: jest.fn() };
      },
    };
  });
  Backbone.Model.extend = function (protoProps) {
    function Model() {
      if (typeof this.initialize === 'function') {
        this.initialize.apply(this, arguments);
      }
    }
    Model.prototype = Object.assign({}, Backbone.Model.prototype, protoProps);
    Model.extend = Backbone.Model.extend;
    return Model;
  };
  return Backbone;
});

jest.mock('goatApp/model/HTMLContentModel', () => {
  const Backbone = require('backbone');
  return Backbone.Model.extend({
    set: function (key, value) {
      this._data = this._data || {};
      this._data[key] = value;
    },
    get: function (key) {
      return this._data && this._data[key];
    },
    trigger: jest.fn(),
  });
});

const LessonContentModelFactory = require('../../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');

describe('LessonContentModel.js delta tests  safer regex and URL handling', () => {
  let LessonContentModel;

  beforeAll(() => {
    LessonContentModel = LessonContentModelFactory;
  });

  test('setContent derives lessonUrl ending with .lesson and pageNum from trailing digits', () => {
    const model = new LessonContentModel();
    global.document = { URL: 'http://example.com/lesson/attack.lesson/42' };
    model.setContent('<html>content</html>');
    const lessonUrl = model.get('lessonUrl');
    const pageNum = model.get('pageNum');
    expect(lessonUrl.endsWith('.lesson')).toBe(true);
    expect(lessonUrl).toBe('http://example.com/lesson/attack.lesson');
    expect(pageNum).toBe(42);
  });

  test('setContent with URL without page segment sets pageNum to 0 and normalizes .lesson', () => {
    const model = new LessonContentModel();
    global.document = { URL: 'http://example.com/lesson/attack.lesson' };
    model.setContent('<html>content</html>');
    const lessonUrl = model.get('lessonUrl');
    const pageNum = model.get('pageNum');
    expect(lessonUrl).toBe('http://example.com/lesson/attack.lesson');
    expect(pageNum).toBe(0);
  });
});
