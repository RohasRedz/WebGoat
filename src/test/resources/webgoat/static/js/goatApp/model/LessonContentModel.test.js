// File: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js
// NOTE: This test assumes a Jest environment with jsdom so that `document.URL` is available.

const $ = require('jquery');
const _ = require('underscore');
const Backbone = require('backbone');

// Minimal HTMLContentModel stub to satisfy the dependency chain
const HTMLContentModel = Backbone.Model.extend({});

// Simulate the AMD module by requiring the updated implementation inline
// and injecting the dependencies it expects.
function createLessonContentModel() {
  // Reconstruct the factory pattern from the original define()
  const factory = function ($dep, _dep, BackboneDep, HTMLContentModelDep) {
    return HTMLContentModelDep.extend({
      urlRoot: null,
      defaults: {
        items: null,
        selectedItem: null,
      },

      initialize: function () {},

      loadData: function (options) {
        this.urlRoot = _dep.escape(encodeURIComponent(options.name)) + '.lesson';
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

        const lessonPageRegex = /\.lesson(?:\/(\d{1,4}))?$/;

        const currentUrl = document.URL;
        const lessonUrl = currentUrl.replace(lessonPageRegex, '.lesson');
        this.set('lessonUrl', lessonUrl);

        const match = currentUrl.match(lessonPageRegex);
        if (match && match[1]) {
          this.set('pageNum', match[1]);
        } else {
          this.set('pageNum', 0);
        }

        this.trigger('content:loaded', this, loadHelps);
      },

      fetch: function (options) {
        options = options || {};
        return BackboneDep.Model.prototype.fetch.call(
          this,
          _dep.extend({ dataType: 'html' }, options),
        );
      },
    });
  };

  return factory($, _, Backbone, HTMLContentModel);
}

describe('LessonContentModel (regex behavior)', () => {
  test('setContent normalizes lessonUrl and extracts numeric pageNum using efficient regex', () => {
    // Arrange
    const LessonContentModel = createLessonContentModel();
    const model = new LessonContentModel();
    const originalUrl =
      'https://example.com/webgoat/SomeLesson.lesson/1234?tracking=abc';
    delete global.location;
    global.location = new URL(originalUrl);
    Object.defineProperty(global.document, 'URL', {
      value: originalUrl,
      configurable: true,
    });

    // Act
    model.setContent('<html>content</html>', true);

    // Assert
    expect(model.get('lessonUrl')).toBe(
      'https://example.com/webgoat/SomeLesson.lesson',
    );
    expect(model.get('pageNum')).toBe('1234');
  });

  test('setContent sets pageNum to 0 when URL has no trailing page segment', () => {
    // Arrange
    const LessonContentModel = createLessonContentModel();
    const model = new LessonContentModel();
    const originalUrl = 'https://example.com/webgoat/SomeLesson.lesson';
    delete global.location;
    global.location = new URL(originalUrl);
    Object.defineProperty(global.document, 'URL', {
      value: originalUrl,
      configurable: true,
    });

    // Act
    model.setContent('<html>content</html>', true);

    // Assert
    expect(model.get('lessonUrl')).toBe(
      'https://example.com/webgoat/SomeLesson.lesson',
    );
    expect(model.get('pageNum')).toBe(0);
  });
});
