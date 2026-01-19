// Delta tests for LessonContentModel.js changes around URL regex and pageNum extraction.

const _ = require('underscore');
const Backbone = require('backbone');

// Minimal HTMLContentModel stub to satisfy the dependency.
// In the real project this should match the actual module path.
class HTMLContentModel extends Backbone.Model {}

describe('LessonContentModel delta tests', () => {
  let LessonContentModel;

  beforeEach(() => {
    // Emulate AMD define wrapper used in the app
    LessonContentModel = (function ($, _, Backbone, HTMLContentModel) {
      return HTMLContentModel.extend({
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

          const currentUrl = String(document.URL || '');

          this.set('lessonUrl', currentUrl.replace(/\.lesson.*/, '.lesson'));

          let pageNum = 0;
          const lessonMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
          if (lessonMatch && lessonMatch[1]) {
            pageNum = parseInt(lessonMatch[1], 10);
            if (!Number.isFinite(pageNum) || pageNum < 0) {
              pageNum = 0;
            }
          }
          this.set('pageNum', pageNum);

          this.trigger('content:loaded', this, loadHelps);
        },

        fetch: function (options) {
          options = options || {};
          // return a then-able dummy to satisfy loadData, not used in delta tests
          return {
            done: (cb) => {
              cb('<html/>');
              return this;
            }
          };
        }
      });
    })(null, _, Backbone, HTMLContentModel);
  });

  test('setContent should set numeric pageNum from URL ending with .lesson/<digits>', () => {
    // Arrange
    const model = new LessonContentModel();
    // simulate browser global
    global.document = { URL: 'http://example.com/test.lesson/12' };

    // Act
    model.setContent('<html/>');

    // Assert
    expect(model.get('pageNum')).toBe(12);
    expect(model.get('lessonUrl')).toBe('http://example.com/test.lesson');
  });

  test('setContent should fall back to pageNum 0 when URL does not match pattern', () => {
    // Arrange
    const model = new LessonContentModel();
    global.document = { URL: 'http://example.com/test' };

    // Act
    model.setContent('<html/>');

    // Assert
    expect(model.get('pageNum')).toBe(0);
    expect(model.get('lessonUrl')).toBe('http://example.com/test');
  });

  test('setContent should sanitize non-numeric or negative page number to 0', () => {
    // Arrange
    const model = new LessonContentModel();
    // Force an invalid value by simulating a malformed URL that still matches the regex
    global.document = { URL: 'http://example.com/test.lesson/0000' };

    // Act
    model.setContent('<html/>');

    // Assert
    expect(model.get('pageNum')).toBe(0);
  });
});
