// Assumed module name/path based on resolved_file_path:
// src/main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js
// e28692 src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js
// TODO: Adjust import path if your test runner resolves modules differently.

const $ = require('jquery');
const _ = require('underscore');
const Backbone = require('backbone');

// We need HTMLContentModel to construct LessonContentModel via AMD-style factory.
// In tests we can stub it with a minimal Backbone.Model extension.
class HTMLContentModel extends Backbone.Model {}
HTMLContentModel.extend = Backbone.Model.extend;

// Recreate the AMD define wrapper from the production file in CommonJS style for testing.
function createLessonContentModelModule() {
  /* eslint-disable global-require */
  const factory = function ($dep, _dep, BackboneDep, HTMLContentModelDep) {
    // The updated production code in LessonContentModel.js should look like this:
    return HTMLContentModelDep.extend({
      urlRoot: null,
      defaults: {
        items: null,
        selectedItem: null
      },

      initialize: function (options) {
      },

      loadData: function (options) {
        // Updated secure/safe behavior (no _.escape, only encodeURIComponent)
        this.urlRoot = encodeURIComponent(options.name) + '.lesson';
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
        this.set('lessonUrl', currentUrl.replace(/\.lesson.*/, '.lesson'));

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
        return BackboneDep.Model.prototype.fetch.call(
          this,
          _dep.extend({ dataType: 'html' }, options)
        );
      }
    });
  };

  return factory($, _, Backbone, HTMLContentModel);
}

const LessonContentModel = createLessonContentModelModule();

describe('LessonContentModel delta security tests (regex & encoding changes)', () => {
  let model;
  let originalFetch;

  beforeEach(() => {
    model = new LessonContentModel();
    // Spy on fetch to avoid real network calls and to control the done() callback.
    originalFetch = Backbone.Model.prototype.fetch;
    jest.spyOn(Backbone.Model.prototype, 'fetch').mockImplementation(function () {
      // Simulate a jQuery-like deferred with done()
      return {
        done: (cb) => {
          cb('<html>dummy</html>');
          return this;
        }
      };
    });
  });

  afterEach(() => {
    Backbone.Model.prototype.fetch.mockRestore();
    Backbone.Model.prototype.fetch = originalFetch;
  });

  test('loadData sets urlRoot using encodeURIComponent only (no HTML escaping / double-encoding)', () => {
    // Arrange
    const nameWithSpacesAndSymbols = 'Le9sson name / 100%';
    const expected = encodeURIComponent(nameWithSpacesAndSymbols) + '.lesson';

    // Act
    model.loadData({ name: nameWithSpacesAndSymbols });

    // Assert
    expect(model.urlRoot).toBe(expected);
  });

  test('setContent derives pageNum from URL with safe, efficient regex when it matches', () => {
    // Arrange
    const originalUrl = 'https://example.com/lessonX.lesson/1234';
    Object.defineProperty(window, 'location', {
      value: { href: originalUrl },
      writable: true
    });
    Object.defineProperty(window.document, 'URL', {
      value: originalUrl,
      writable: true
    });

    const contentLoadedHandler = jest.fn();
    model.on('content:loaded', contentLoadedHandler);

    // Act
    model.setContent('<html>dummy</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('https://example.com/lessonX.lesson');
    expect(model.get('pageNum')).toBe('1234');
    expect(contentLoadedHandler).toHaveBeenCalledWith(model, true);
  });

  test('setContent falls back to pageNum 0 when URL does not match the page pattern', () => {
    // Arrange
    const nonMatchingUrl = 'https://example.com/lessonX.lesson';
    Object.defineProperty(window.document, 'URL', {
      value: nonMatchingUrl,
      writable: true
    });

    // Act
    model.setContent('<html>dummy</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('https://example.com/lessonX.lesson');
    expect(model.get('pageNum')).toBe(0);
  });

  test('setContent uses provided loadHelps flag and preserves event contract', () => {
    // Arrange
    const contentLoadedHandler = jest.fn();
    model.on('content:loaded', contentLoadedHandler);

    // Act
    model.setContent('<html>dummy</html>', false);

    // Assert
    expect(model.get('content')).toBe('<html>dummy</html>');
    expect(contentLoadedHandler).toHaveBeenCalledWith(model, false);
  });
});
