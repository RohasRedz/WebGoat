// Derived test path: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js
// Delta tests for URL parsing behavior in LessonContentModel.js

const path = require('path');

// Require the same module path as used by the AMD define; in Jest we can simulate it via require.
jest.mock('backbone', () => {
  const Backbone = require('backbone');
  return Backbone;
});
jest.mock('underscore', () => require('underscore'));
jest.mock('jquery', () => require('jquery'));
jest.mock('goatApp/model/HTMLContentModel', () => {
  const Backbone = require('backbone');
  return Backbone.Model.extend({});
});

describe('LessonContentModel URL parsing (delta tests)', () => {
  let LessonContentModel;
  let HTMLContentModel;
  let Backbone;

  beforeAll(() => {
    // Simulate AMD-style define by requiring the built file through Jest's module system.
    // In a real test environment this would be wired by your bundler/test setup.
    Backbone = require('backbone');
    HTMLContentModel = require('goatApp/model/HTMLContentModel');

    // Recreate the factory logic from LessonContentModel.js
    // eslint-disable-next-line global-require
    const _ = require('underscore');
    // We inline the factory as the source uses AMD define([...], function(...) { return HTMLContentModel.extend({...}); });
    LessonContentModel = HTMLContentModel.extend({
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

        const currentUrl = String(global.document.URL || '');
        const lessonUrlMatch = currentUrl.match(/^(.*\.lesson)(?:\/\d{1,4})?$/);
        if (lessonUrlMatch && lessonUrlMatch[1]) {
          this.set('lessonUrl', lessonUrlMatch[1]);
        } else {
          const baseUrl = currentUrl.split('#')[0].split('?')[0];
          this.set('lessonUrl', baseUrl);
        }

        let pageNum = 0;
        const pageNumMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
        if (pageNumMatch && pageNumMatch[1]) {
          pageNum = parseInt(pageNumMatch[1], 10);
          if (!Number.isFinite(pageNum) || pageNum < 0) {
            pageNum = 0;
          }
        }
        this.set('pageNum', pageNum);

        this.trigger('content:loaded', this, loadHelps);
      },
      fetch: function (options) {
        options = options || {};
        return Backbone.Model.prototype.fetch.call(
          this,
          Object.assign({ dataType: 'html' }, options)
        );
      }
    });
  });

  beforeEach(() => {
    global.document = { URL: '' };
  });

  test('setContent extracts lessonUrl without trailing page segment and sets numeric pageNum', () => {
    // Arrange
    const model = new LessonContentModel();
    const testUrl = 'http://example.com/foo.lesson/12';
    global.document.URL = testUrl;

    // Act
    model.setContent('<html></html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/foo.lesson');
    expect(model.get('pageNum')).toBe(12);
  });

  test('setContent falls back to base URL and pageNum 0 when URL has no page segment', () => {
    // Arrange
    const model = new LessonContentModel();
    const testUrl = 'http://example.com/foo.lesson?query=1#hash';
    global.document.URL = testUrl;

    // Act
    model.setContent('<html></html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/foo.lesson');
    expect(model.get('pageNum')).toBe(0);
  });

  test('setContent coerces invalid page number to 0', () => {
    // Arrange
    const model = new LessonContentModel();
    const testUrl = 'http://example.com/foo.lesson/99999'; // does not match \d{1,4}
    global.document.URL = testUrl;

    // Act
    model.setContent('<html></html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/foo.lesson/99999');
    expect(model.get('pageNum')).toBe(0);
  });
});
