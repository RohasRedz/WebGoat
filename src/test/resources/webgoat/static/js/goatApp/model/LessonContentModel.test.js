const _ = require('underscore');
const Backbone = require('backbone');

// Minimal HTMLContentModel stub to allow extension
class HTMLContentModel extends Backbone.Model {}

const LessonContentModelFactory = () => {
  // Inline the updated module content logic
  return HTMLContentModel.extend({
    urlRoot: null,
    defaults: {
      items: null,
      selectedItem: null,
    },

    initialize: function () {},

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

      let url = String(global.document.URL || '');
      let lessonUrl = url;
      let pageNum = 0;

      try {
        const parsed = new URL(url, 'http://example.com');
        lessonUrl =
          parsed.origin +
          parsed.pathname.replace(/\.lesson\/?\d*$/, '.lesson');
        const segments = parsed.pathname.split('/');
        const lastSegment = segments[segments.length - 1];
        if (/^\d{1,4}$/.test(lastSegment)) {
          pageNum = parseInt(lastSegment, 10);
        }
      } catch (e) {
        lessonUrl = url.replace(/\.lesson\/?\d*$/, '.lesson');
        const simpleMatch = url.match(/\.lesson\/(\d{1,4})$/);
        if (simpleMatch && simpleMatch[1]) {
          pageNum = parseInt(simpleMatch[1], 10);
        }
      }

      this.set('lessonUrl', lessonUrl);
      this.set('pageNum', isFinite(pageNum) ? pageNum : 0);

      this.trigger('content:loaded', this, loadHelps);
    },

    fetch: function (options) {
      options = options || {};
      // Simulate Backbone fetch returning a jQuery-like Deferred
      return {
        done: (cb) => {
          cb('<html></html>');
          return this;
        },
      };
    },
  });
};

describe('LessonContentModel URL parsing (delta tests)', () => {
  let LessonContentModel;

  beforeEach(() => {
    LessonContentModel = LessonContentModelFactory();
    global.document = { URL: 'http://localhost:8080/foo.lesson/3' };
  });

  afterEach(() => {
    delete global.document;
  });

  test('setContent derives lessonUrl and pageNum from typical URL', () => {
    const model = new LessonContentModel();
    const listener = jest.fn();
    model.on('content:loaded', listener);

    model.setContent('<html/>');

    expect(model.get('lessonUrl')).toBe('http://localhost:8080/foo.lesson');
    expect(model.get('pageNum')).toBe(3);
    expect(listener).toHaveBeenCalledWith(model, true);
  });

  test('setContent defaults pageNum to 0 when no page segment present', () => {
    global.document.URL = 'http://localhost:8080/foo.lesson';
    const model = new LessonContentModel();

    model.setContent('<html/>');

    expect(model.get('lessonUrl')).toBe('http://localhost:8080/foo.lesson');
    expect(model.get('pageNum')).toBe(0);
  });

  test('setContent handles unusual but valid URLs without throwing', () => {
    global.document.URL =
      'http://localhost:8080/strange-path/foo.lesson/9999?param=a';
    const model = new LessonContentModel();

    expect(() => model.setContent('<html/>')).not.toThrow();
    expect(model.get('lessonUrl')).toContain('foo.lesson');
    expect(model.get('pageNum')).toBe(9999);
  });
});
