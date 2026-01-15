// File: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

const $ = require('jquery');
const _ = require('underscore');
const Backbone = require('backbone');

class HTMLContentModel extends Backbone.Model {}

const LessonContentModelFactory = () => {
  const LessonContentModel = HTMLContentModel.extend({
    urlRoot: null,
    defaults: {
      items: null,
      selectedItem: null
    },

    initialize: function (options) {},

    loadData: function (options) {
      this.urlRoot = _.escape(encodeURIComponent(options.name)) + '.lesson';
      const self = this;
      this.fetch().done(function (data) {
        self.setContent(data);
      });
    },

    setContent: function (content, loadHelps) {
      if (typeof loadHelps === 'undefined') {
        loadHelps = true;
      }
      this.set('content', content);
      const lessonUrl = document.URL.replace(/\.lesson.*/, '.lesson');
      this.set('lessonUrl', lessonUrl);
      const pageNumMatch = lessonUrl.match(/\.lesson\/(\d{1,4})$/);
      if (pageNumMatch && pageNumMatch[1]) {
        this.set('pageNum', pageNumMatch[1]);
      } else {
        this.set('pageNum', 0);
      }
      this.trigger('content:loaded', this, loadHelps);
    },

    fetch: function (options) {
      options = options || {};
      return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: 'html' }, options));
    }
  });

  return LessonContentModel;
};

describe('LessonContentModel – regex hardening for pageNum parsing', () => {
  let originalLocation;

  beforeAll(() => {
    originalLocation = global.window ? global.window.location : undefined;
    if (!global.window) {
      global.window = {};
    }
    global.window.location = { href: 'http://localhost' };
    global.document = {
      get URL() {
        return global.window.location.href;
      }
    };
  });

  afterAll(() => {
    if (originalLocation) {
      global.window.location = originalLocation;
    }
  });

  test('setContent extracts numeric pageNum from lesson URL using bounded regex', () => {
    const LessonContentModel = LessonContentModelFactory();
    const model = new LessonContentModel();

    global.window.location.href = 'http://example.com/foo.lesson/123';

    const loadedSpy = jest.fn();
    model.on('content:loaded', loadedSpy);

    model.setContent('<html/>');

    expect(model.get('lessonUrl')).toBe('http://example.com/foo.lesson');
    expect(model.get('pageNum')).toBe('123');
    expect(loadedSpy).toHaveBeenCalled();
  });

  test('setContent falls back to pageNum 0 when URL does not match pattern', () => {
    const LessonContentModel = LessonContentModelFactory();
    const model = new LessonContentModel();

    global.window.location.href = 'http://example.com/foo';

    model.setContent('<html/>');

    expect(model.get('lessonUrl')).toBe('http://example.com/foo');
    expect(model.get('pageNum')).toBe(0);
  });

  test('setContent handles long or adversarial URLs without catastrophic regex behavior', () => {
    const LessonContentModel = LessonContentModelFactory();
    const model = new LessonContentModel();

    const longSegment = 'a'.repeat(5000);
    global.window.location.href = `http://example.com/${longSegment}.lesson`;

    const start = Date.now();
    model.setContent('<html/>');
    const elapsed = Date.now() - start;

    expect(elapsed).toBeLessThan(500);
    expect(model.get('lessonUrl')).toBe(
      `http://example.com/${longSegment}.lesson`
    );
    expect(model.get('pageNum')).toBe(0);
  });
});
