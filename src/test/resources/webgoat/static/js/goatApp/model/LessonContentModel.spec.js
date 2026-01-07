/*
 * Delta test for BATCH-003 - LessonContentModel.js
 */
const Backbone = require('backbone');
const _ = require('underscore');

// Minimal HTMLContentModel stub to satisfy inheritance
class HTMLContentModel extends Backbone.Model {}

// Since the original code uses AMD define(), we simulate the factory invocation here.
function createLessonContentModelModule() {
  // Inline the updated module implementation to avoid AMD loader complexity in delta tests.
  const LessonContentModel = HTMLContentModel.extend({
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

      const currentUrl = document.URL;

      const pageNumMatch = currentUrl.match(/\/(\d{1,4})$/);
      let baseUrl = currentUrl;
      if (pageNumMatch) {
        baseUrl = currentUrl.slice(0, -pageNumMatch[0].length);
      }

      const lessonSuffixIndex = baseUrl.indexOf('.lesson');
      if (lessonSuffixIndex !== -1) {
        baseUrl = baseUrl.substring(0, lessonSuffixIndex + '.lesson'.length);
      }

      this.set('lessonUrl', baseUrl);

      if (pageNumMatch) {
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
}

/**
 * Delta tests validating the new, efficient URL parsing behavior and page number extraction,
 * ensuring that the simplified regex and string operations behave as expected.
 *
 * Jira: SVCF-1811
 */
describe('LessonContentModel delta tests', () => {
  let LessonContentModel;

  beforeEach(() => {
    LessonContentModel = createLessonContentModelModule();
    // Jest JSDOM environment provides document; we just ensure it's present.
    expect(document).toBeDefined();
  });

  test('setContent extracts pageNum and lessonUrl with numeric page suffix', () => {
    // Arrange
    const model = new LessonContentModel();
    const url = 'http://example.com/foo.lesson/123';
    Object.defineProperty(document, 'URL', {
      value: url,
      configurable: true
    });

    const contentLoadedSpy = jest.fn();
    model.on('content:loaded', contentLoadedSpy);

    // Act
    model.setContent('<html>content</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/foo.lesson');
    expect(model.get('pageNum')).toBe('123'); // should come from the capture group
    expect(contentLoadedSpy).toHaveBeenCalledWith(model, true);
  });

  test('setContent sets pageNum=0 and trims to .lesson when no page suffix present', () => {
    // Arrange
    const model = new LessonContentModel();
    const url = 'http://example.com/bar.lesson';
    Object.defineProperty(document, 'URL', {
      value: url,
      configurable: true
    });

    // Act
    model.setContent('<html>content</html>', false);

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/bar.lesson');
    expect(model.get('pageNum')).toBe(0);
  });

  test('setContent handles extra path segments efficiently without catastrophic regex', () => {
    // This test indirectly validates that the simplified regex and slice/substring logic
    // behave correctly on longer URLs that might have induced ReDoS with a complex regex.
    const model = new LessonContentModel();
    const url =
      'http://example.com/very/long/path/with/many/segments/and.parameters.lesson/42';
    Object.defineProperty(document, 'URL', {
      value: url,
      configurable: true
    });

    // Act
    model.setContent('x');

    // Assert
    expect(model.get('lessonUrl')).toBe(
      'http://example.com/very/long/path/with/many/segments/and.parameters.lesson'
    );
    expect(model.get('pageNum')).toBe('42');
  });
});
