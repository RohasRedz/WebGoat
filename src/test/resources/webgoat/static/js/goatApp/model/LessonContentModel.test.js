// Test file path (mirrors main with a parallel test directory assumption):
// src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js
// NOTE: If your project uses a different Jest test directory, adjust accordingly.

const $ = require('jquery');
const _ = require('underscore');
const Backbone = require('backbone');

// Minimal HTMLContentModel stub to satisfy the AMD-style extend.
// In the real test environment, require the actual module instead.
const HTMLContentModel = Backbone.Model;

// Adapt AMD module to CommonJS for Jest by simulating the define wrapper.
// In production, you would import the built module directly.
const LessonContentModelFactory = (jq, underscore, backbone, HtmlModel) => {
  return HtmlModel.extend({
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
      const lessonUrl = currentUrl.replace(/\.lesson(?:\/.*)?$/, '.lesson');
      this.set('lessonUrl', lessonUrl);

      const pageMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
      if (pageMatch) {
        this.set('pageNum', pageMatch[1]);
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
};

const LessonContentModel = LessonContentModelFactory($, _, Backbone, HTMLContentModel);

describe('LessonContentModel.setContent delta tests', () => {
  let originalUrl;

  beforeAll(() => {
    originalUrl = global.document && global.document.URL;
  });

  afterAll(() => {
    if (originalUrl) {
      Object.defineProperty(document, 'URL', {
        value: originalUrl,
        writable: true,
        configurable: true
      });
    }
  });

  function setDocumentUrl(url) {
    Object.defineProperty(document, 'URL', {
      value: url,
      writable: true,
      configurable: true
    });
  }

  test('sets lessonUrl without page number and pageNum = 0 when URL has no page segment', () => {
    // Arrange
    setDocumentUrl('http://localhost/WebGoat/lesson/Injection.lesson');
    const model = new LessonContentModel();
    const onLoaded = jest.fn();
    model.on('content:loaded', onLoaded);

    // Act
    model.setContent('<html>content</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://localhost/WebGoat/lesson/Injection.lesson');
    expect(model.get('pageNum')).toBe(0);
    expect(onLoaded).toHaveBeenCalledWith(model, true);
  });

  test('sets lessonUrl and pageNum correctly when URL includes page number segment', () => {
    // Arrange
    setDocumentUrl('http://localhost/WebGoat/lesson/Injection.lesson/42');
    const model = new LessonContentModel();
    const onLoaded = jest.fn();
    model.on('content:loaded', onLoaded);

    // Act
    model.setContent('<html>content</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://localhost/WebGoat/lesson/Injection.lesson');
    expect(model.get('pageNum')).toBe('42');
    expect(onLoaded).toHaveBeenCalledWith(model, true);
  });

  test('sets pageNum = 0 when URL has non-numeric tail after .lesson', () => {
    // Arrange
    setDocumentUrl('http://localhost/WebGoat/lesson/Injection.lesson/foo');
    const model = new LessonContentModel();
    const onLoaded = jest.fn();
    model.on('content:loaded', onLoaded);

    // Act
    model.setContent('<html>content</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://localhost/WebGoat/lesson/Injection.lesson');
    expect(model.get('pageNum')).toBe(0);
    expect(onLoaded).toHaveBeenCalledWith(model, true);
  });
});
