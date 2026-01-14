// Derived test path: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

const $ = require('jquery');
const _ = require('underscore');
const Backbone = require('backbone');

// Minimal HTMLContentModel stub to allow LessonContentModel to extend it
const HTMLContentModel = Backbone.Model.extend({});

// Simulate AMD define wrapper from the application.
// In actual project setup, LessonContentModel would be loaded through RequireJS/AMD.
// Here we require the module directly by relative path as Node-style, which corresponds
// to src/test/resources vs src/main/resources layout.
// TODO: Adjust relative path if your Jest module resolution differs.
const LessonContentModel = (function loadModule() {
  // The fixed module is assumed to be transpiled/available under the same relative path,
  // replacing 'main' with 'test' only for file placement, not runtime resolution.
  return require('../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js')($, _, Backbone, HTMLContentModel);
})();

describe('LessonContentModel delta tests (regex and URL handling hardening)', () => {
  test('setContent derives lessonUrl with bounded regex without catastrophic backtracking', () => {
    // Arrange
    const model = new LessonContentModel();
    const originalHref = global.location && global.location.href;

    // Use a long URL that previously could cause heavy regex work; now it should be bounded.
    const longPath = '/some/really/long/path/'.repeat(50);
    const url = `https://example.com${longPath}my.lesson/1234`;
    delete global.location;
    global.location = { href: url };

    const content = '<html>dummy</html>';

    // Act
    model.setContent(content, true);

    // Assert
    expect(model.get('lessonUrl')).toBe(
      url.replace(/\.lesson\/1234$/, '.lesson')
    );
    expect(model.get('pageNum')).toBe('1234');

    // Cleanup
    if (originalHref !== undefined) {
      global.location.href = originalHref;
    }
  });

  test('setContent sets pageNum to 0 when URL does not end with .lesson/<digits>', () => {
    const model = new LessonContentModel();
    const originalHref = global.location && global.location.href;
    const url = 'https://example.com/app/lesson/start';
    delete global.location;
    global.location = { href: url };

    const content = '<html>dummy</html>';

    model.setContent(content, true);

    expect(model.get('pageNum')).toBe(0);
    expect(model.get('lessonUrl')).toBe(
      url.replace(/\.lesson(?:\/\d{1,4})?$/, '.lesson')
    );

    if (originalHref !== undefined) {
      global.location.href = originalHref;
    }
  });

  test('loadData safely handles missing options.name without throwing and still sets urlRoot', () => {
    const model = new LessonContentModel();

    expect(() => model.loadData({})).not.toThrow();
    expect(typeof model.urlRoot).toBe('string');
  });
});
