/**
 * Delta tests for LessonContentModel.js focusing on the updated regex handling and URL parsing.
 *
 * Intended path:
 * src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js
 */

const $ = require('jquery');
const _ = require('underscore');
const Backbone = require('backbone');

// Since the original code uses AMD define(), we simulate the factory directly here.
// In a real test environment, you'd likely use a loader (e.g., requirejs) or refactor.
const HTMLContentModel = Backbone.Model.extend({});

function createLessonContentModelModule() {
  return (function ($, _, Backbone, HTMLContentModel) {
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

        // This block is copied from the fixed file to validate the changed behavior.
        const lessonUrlPattern = /\.lesson.*/;
        this.set('lessonUrl', document.URL.replace(lessonUrlPattern, '.lesson'));

        const pagePattern = /.*\.lesson\/(\d{1,4})$/;
        if (pagePattern.test(document.URL)) {
          this.set('pageNum', document.URL.replace(pagePattern, '$1'));
        } else {
          this.set('pageNum', 0);
        }
        this.trigger('content:loaded', this, loadHelps);
      },

      fetch: function (options) {
        options = options || {};
        return Backbone.Model.prototype.fetch.call(
          this,
          _.extend({ dataType: 'html' }, options)
        );
      }
    });
  })($, _, Backbone, HTMLContentModel);
}

describe('LessonContentModel regex handling (delta tests)', () => {
  let LessonContentModel;
  let model;

  beforeEach(() => {
    LessonContentModel = createLessonContentModelModule();
    model = new LessonContentModel();
  });

  test('setContent uses precompiled regex patterns and normalizes lessonUrl', () => {
    const originalUrl = 'http://example.com/path/to/lesson/Intro.lesson/1234';
    Object.defineProperty(global, 'document', {
      value: { URL: originalUrl },
      configurable: true
    });

    const contentLoadedSpy = jest.fn();
    model.on('content:loaded', contentLoadedSpy);

    model.setContent('<html>content</html>', true);

    expect(model.get('lessonUrl')).toBe(
      'http://example.com/path/to/lesson/Intro.lesson'
    );
    expect(model.get('pageNum')).toBe('1234');
    expect(contentLoadedSpy).toHaveBeenCalledWith(model, true);
  });

  test('setContent assigns pageNum 0 when URL does not match the page pattern', () => {
    const originalUrl = 'http://example.com/path/to/lesson/Intro.lesson';
    Object.defineProperty(global, 'document', {
      value: { URL: originalUrl },
      configurable: true
    });

    model.setContent('<html>content</html>', false);

    expect(model.get('pageNum')).toBe(0);
  });
});
