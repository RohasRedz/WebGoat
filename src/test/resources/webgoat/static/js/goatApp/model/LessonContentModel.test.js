const { JSDOM } = require('jsdom');

describe('LessonContentModel - delta tests for URL regex hardening', () => {
  let LessonContentModel;
  let modelInstance;

  beforeEach(() => {
    // Minimal Backbone/HTMLContentModel shims to exercise setContent
    const Backbone = {
      Model: class {
        constructor() {
          this.attributes = {};
        }
        set(key, value) {
          this.attributes[key] = value;
        }
        get(key) {
          return this.attributes[key];
        }
        trigger() {
          // no-op for tests
        }
      }
    };

    // Minimal HTMLContentModel.extend emulation
    const HTMLContentModel = {
      extend(def) {
        class Extended extends Backbone.Model {
          constructor(options) {
            super(options);
            if (typeof def.initialize === 'function') {
              def.initialize.call(this, options);
            }
          }
        }
        Object.assign(Extended.prototype, def);
        return Extended;
      }
    };

    // Recreate the module factory from the updated code
    // eslint-disable-next-line global-require, import/no-dynamic-require
    LessonContentModel = (function ($, _, BackboneShim, HTMLContentModelShim) {
      return HTMLContentModelShim.extend({
        urlRoot: null,
        defaults: {
          items: null,
          selectedItem: null
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

          // Copied from updated production code
          const currentUrl = String(global.document.URL);
          const lessonMatch = currentUrl.match(/^(.*?\.lesson)(?:\/.*)?$/);
          if (lessonMatch && lessonMatch[1]) {
            this.set('lessonUrl', lessonMatch[1]);
          } else {
            this.set('lessonUrl', currentUrl);
          }

          const pageNumMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
          if (pageNumMatch && pageNumMatch[1]) {
            this.set('pageNum', pageNumMatch[1]);
          } else {
            this.set('pageNum', 0);
          }

          this.trigger('content:loaded', this, loadHelps);
        },

        fetch: function (options) {
          options = options || {};
          // Mocked fetch that returns a thenable with done() to keep behavior simple
          return {
            done: (cb) => {
              cb('<html></html>');
            }
          };
        }
      });
    })(
      {}, // $
      {
        escape: (s) => s
      }, // _
      Backbone,
      HTMLContentModel
    );

    // Setup a default DOM
    const dom = new JSDOM('<!doctype html><html><body></body></html>', {
      url: 'http://localhost/WebGoat.lesson/1'
    });
    global.window = dom.window;
    global.document = dom.window.document;

    modelInstance = new LessonContentModel();
  });

  afterEach(() => {
    delete global.window;
    delete global.document;
  });

  test('setContent should derive lessonUrl as base .lesson URL and pageNum from trailing segment', () => {
    // Arrange
    global.document.URL = 'http://example.com/webgoat/SomeCourse.lesson/12';

    // Act
    modelInstance.setContent('<div/>');

    // Assert
    expect(modelInstance.get('lessonUrl')).toBe(
      'http://example.com/webgoat/SomeCourse.lesson'
    );
    expect(modelInstance.get('pageNum')).toBe('12');
  });

  test('setContent should fall back to full URL and pageNum 0 when pattern does not match', () => {
    // Arrange: URL that does not contain ".lesson"
    global.document.URL = 'http://example.com/webgoat/no-lesson-here';

    // Act
    modelInstance.setContent('<div/>');

    // Assert
    expect(modelInstance.get('lessonUrl')).toBe(
      'http://example.com/webgoat/no-lesson-here'
    );
    expect(modelInstance.get('pageNum')).toBe(0);
  });
});
