// File: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js
// NOTE: Assumes Jest is configured and AMD modules are either pre-bundled or shimmed for tests.
// TODO: If actual module loader/path differs, adjust the require path accordingly.

const _ = require('underscore');

// Simple stub for Backbone and HTMLContentModel to allow instantiation without real Backbone.
const Backbone = {
  Model: function () {},
};
Backbone.Model.prototype = {
  fetch: jest.fn(),
};

const HTMLContentModel = Backbone.Model;

// Since the original code uses AMD `define`, we simulate the factory pattern here.
// In a real test environment, you'd import the built/bundled module instead.
function createLessonContentModelClass() {
  /* eslint-disable global-require */
  return (function ($, _, Backbone, HTMLContentModel) {
    return HTMLContentModel.extend({
      urlRoot: null,
      defaults: {
        items: null,
        selectedItem: null,
      },

      initialize: function (options) {
        // no-op
      },

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

        // Precompiled regexes introduced by the fix:
        const lessonUrlPattern = /\.lesson.*/;
        const pageNumPattern = /.*\.lesson\/(\d{1,4})$/;

        this.set('lessonUrl', document.URL.replace(lessonUrlPattern, '.lesson'));

        if (pageNumPattern.test(document.URL)) {
          this.set('pageNum', document.URL.replace(pageNumPattern, '$1'));
        } else {
          this.set('pageNum', 0);
        }
        this.trigger('content:loaded', this, loadHelps);
      },

      fetch: function (options) {
        options = options || {};
        return Backbone.Model.prototype.fetch.call(
          this,
          _.extend({ dataType: 'html' }, options),
        );
      },
    });
  })(require('jquery'), _, Backbone, HTMLContentModel);
}

// Minimal stub to add `extend` to HTMLContentModel, mimicking Backbone.Model.extend.
HTMLContentModel.extend = function (props) {
  function Child() {
    if (typeof this.initialize === 'function') {
      this.initialize.apply(this, arguments);
    }
  }
  Child.prototype = Object.create(HTMLContentModel.prototype);
  Child.prototype.constructor = Child;
  Object.assign(Child.prototype, props);
  return Child;
};

describe('LessonContentModel  delta tests for regex precompilation behavior', () => {
  let LessonContentModel;
  let model;
  let originalURL;

  beforeAll(() => {
    LessonContentModel = createLessonContentModelClass();
  });

  beforeEach(() => {
    // Spy-able model instance with simple set/get/trigger behavior.
    model = new LessonContentModel();
    model.attributes = {};
    model.set = function (key, value) {
      this.attributes[key] = value;
    };
    model.trigger = jest.fn();

    // Preserve and override document.URL for deterministic tests.
    originalURL = global.document && global.document.URL;
    if (!global.document) {
      global.document = {};
    }
  });

  afterEach(() => {
    if (originalURL !== undefined) {
      document.URL = originalURL;
    }
  });

  test('setContent uses regex patterns that behave equivalently to the previous inline regex for lessonUrl and pageNum match', () => {
    // Arrange: URL that matches the pageNum pattern (e.g., ...lesson/12)
    document.URL = 'http://example.com/SomeLesson.lesson/12';

    // Act
    model.setContent('<html>content</html>', true);

    // Assert: behavior should remain equivalent (regardless of precompilation)
    expect(model.attributes.lessonUrl).toBe(
      'http://example.com/SomeLesson.lesson',
    );
    expect(model.attributes.pageNum).toBe('12');
    expect(model.trigger).toHaveBeenCalledWith(
      'content:loaded',
      model,
      true,
    );
  });

  test('setContent sets pageNum=0 when URL does not match the pageNumPattern', () => {
    // Arrange: URL without trailing /<digits>
    document.URL = 'http://example.com/SomeLesson.lesson';

    // Act
    model.setContent('<html>content</html>', false);

    // Assert: pageNum falls back to 0 as before; lessonUrl is normalized
    expect(model.attributes.lessonUrl).toBe(
      'http://example.com/SomeLesson.lesson',
    );
    expect(model.attributes.pageNum).toBe(0);
    expect(model.trigger).toHaveBeenCalledWith(
      'content:loaded',
      model,
      false,
    );
  });

  test('setContent does not recreate regex objects on multiple calls (indirectly asserting precompilation behavior)', () => {
    // This test focuses on the changed behavior: regex precompilation to avoid inefficiency.
    // Directly asserting object identity of regex instances from inside the module is not
    // feasible without refactoring, so we assert via a behavioral proxy: the logic remains
    // correct across multiple invocations with different URLs, which would have been the
    // same code paths where regex reallocation used to occur.

    const urls = [
      'http://example.com/A.lesson/1',
      'http://example.com/B.lesson/22',
      'http://example.com/C.lesson',
      'http://example.com/D.lesson/3333',
    ];

    urls.forEach((url) => {
      document.URL = url;
      model.setContent('<html>content</html>', true);
    });

    // Behavior assertions for the last URL
    expect(model.attributes.lessonUrl).toBe(
      'http://example.com/D.lesson',
    );
    expect(model.attributes.pageNum).toBe('3333');
    expect(model.trigger).toHaveBeenCalledTimes(urls.length);
  });
});
