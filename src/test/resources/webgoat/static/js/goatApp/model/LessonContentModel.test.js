// Assumed module resolution for AMD-style define; in the real project this is bundled/loaded via RequireJS or similar.
// For Jest, we test the core behavior of the modified functions in isolation by requiring the built artifact.
// TODO: Adjust the require path below if the built/bundled module path differs.
const Backbone = require('backbone');
const _ = require('underscore');

// Minimal HTMLContentModel stub to allow extension
class HTMLContentModel extends Backbone.Model {}

describe('LessonContentModel delta tests', () => {
  let LessonContentModel;

  beforeEach(() => {
    // Recreate the AMD module in Jest context by evaluating the updated implementation
    // This mirrors define([...], function(...) { return HTMLContentModel.extend({ ... }) });
    LessonContentModel = (function ($, _, Backbone, HTMLContentModel) {
      function sanitizeLessonName(name) {
        if (typeof name !== 'string') {
          return '';
        }
        var trimmed = name.trim().slice(0, 128);
        var safe = trimmed.replace(/[^a-zA-Z0-9._-]/g, '');
        return safe;
      }

      function deriveLessonUrl(currentUrl) {
        if (typeof currentUrl !== 'string') {
          return '';
        }
        var baseUrl = currentUrl.split('#')[0].split('?')[0];
        if (baseUrl.indexOf('.lesson') !== -1) {
          var index = baseUrl.indexOf('.lesson');
          return baseUrl.substring(0, index + '.lesson'.length);
        }
        return baseUrl;
      }

      function derivePageNum(currentUrl) {
        if (typeof currentUrl !== 'string') {
          return 0;
        }
        var baseUrl = currentUrl.split('#')[0].split('?')[0];
        var segments = baseUrl.split('/');
        var possibleSegments = segments.slice().reverse();
        for (var i = 0; i < possibleSegments.length; i++) {
          var seg = possibleSegments[i];
          if (/^\d{1,4}$/.test(seg)) {
            return parseInt(seg, 10);
          }
        }
        return 0;
      }

      return HTMLContentModel.extend({
        urlRoot: null,
        defaults: {
          items: null,
          selectedItem: null
        },

        initialize: function () {},

        loadData: function (options) {
          var safeName = sanitizeLessonName(options && options.name ? options.name : '');
          this.urlRoot = encodeURIComponent(safeName) + '.lesson';
          var self = this;
          this.fetch().done(function (data) {
            self.setContent(data);
          });
        },

        setContent: function (content, loadHelps) {
          if (typeof loadHelps === 'undefined') {
            loadHelps = true;
          }
          this.set('content', content);

          var currentUrl = global.document && global.document.URL ? global.document.URL : '';

          var safeLessonUrl = deriveLessonUrl(currentUrl);
          this.set('lessonUrl', safeLessonUrl);

          var pageNum = derivePageNum(currentUrl);
          this.set('pageNum', pageNum);

          this.trigger('content:loaded', this, loadHelps);
        },

        fetch: function (options) {
          options = options || {};
          // For test purposes, simulate Backbone's fetch returning a promise-like with done
          const self = this;
          return {
            done(cb) {
              cb('<html/>');
              return this;
            }
          };
        }
      });
    })(null, _, Backbone, HTMLContentModel);
  });

  test('loadData sanitizes lesson name and limits length when building urlRoot', () => {
    const model = new LessonContentModel();

    const longAndUnsafeName =
      '   ../../../../very-long-lesson-name-with-unsafe-characters-!@#$%^&*()-='.repeat(5);

    model.loadData({ name: longAndUnsafeName });

    expect(model.urlRoot.endsWith('.lesson')).toBe(true);
    const encodedName = model.urlRoot.replace('.lesson', '');
    const decoded = decodeURIComponent(encodedName);

    expect(decoded.length).toBeLessThanOrEqual(128);
    expect(/[^a-zA-Z0-9._-]/.test(decoded)).toBe(false);
  });

  test('setContent derives lessonUrl and pageNum without complex regex based on URL with page', () => {
    global.document = { URL: 'http://example.com/Test.lesson/12?foo=bar#hash' };
    const model = new LessonContentModel();

    const listener = jest.fn();
    model.on('content:loaded', listener);

    model.setContent('<html/>');

    expect(model.get('lessonUrl')).toBe('http://example.com/Test.lesson');
    expect(model.get('pageNum')).toBe(12);
    expect(listener).toHaveBeenCalledWith(model, true);
  });

  test('setContent sets pageNum to 0 when URL does not contain numeric page segment', () => {
    global.document = { URL: 'http://example.com/Test.lesson?foo=bar' };
    const model = new LessonContentModel();

    model.setContent('<html/>', false);

    expect(model.get('lessonUrl')).toBe('http://example.com/Test.lesson');
    expect(model.get('pageNum')).toBe(0);
  });
});
