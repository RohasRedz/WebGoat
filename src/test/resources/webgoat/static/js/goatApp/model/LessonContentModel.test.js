// Assuming a Jest test environment and that the module can be required by relative path.
// TODO: Adjust the require path to match the actual test setup/bundler resolution.
const { JSDOM } = require('jsdom');

describe('LessonContentModel (delta tests for regex hardening)', () => {
  let window;
  let document;
  let Backbone;
  let HTMLContentModel;
  let LessonContentModel;

  beforeEach(() => {
    const dom = new JSDOM('<!doctype html><html><body></body></html>', {
      url: 'https://example.com/start.lesson/12'
    });
    window = dom.window;
    document = window.document;

    // Minimal Backbone/HTMLContentModel stubs
    Backbone = {
      Model: function () {},
    };
    Backbone.Model.prototype = {
      fetch: jest.fn(function (options) {
        // Simulate jQuery-like deferred with done() callback
        return {
          done: (cb) => {
            cb('<html>content</html>');
          },
        };
      }),
    };

    HTMLContentModel = function () {};
    HTMLContentModel.extend = function (def) {
      function Ctor() {
        this.attributes = {};
        if (typeof def.initialize === 'function') {
          def.initialize.apply(this, arguments);
        }
      }
      Ctor.prototype = Object.assign(
        {
          set: function (k, v) {
            this.attributes[k] = v;
          },
          get: function (k) {
            return this.attributes[k];
          },
          trigger: jest.fn(),
        },
        def
      );
      return Ctor;
    };

    // Simulate AMD define to get the module under test
    global.define = (deps, factory) => {
      LessonContentModel = factory(jest.fn(), { escape: (s) => s }, Backbone, HTMLContentModel);
    };
    global.document = document;

    // Load the module under test
    // eslint-disable-next-line global-require
    require('../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');
  });

  afterEach(() => {
    delete global.define;
    delete global.document;
  });

  test('setContent uses bounded regex to normalize lessonUrl and extract pageNum', () => {
    const model = new LessonContentModel();

    // Act
    model.setContent('<html>content</html>');

    // Assert: for URL ending with `.lesson/12`, lessonUrl should normalize to `.lesson`
    expect(model.get('lessonUrl')).toBe('https://example.com/start.lesson');
    expect(model.get('pageNum')).toBe('12');
  });

  test('setContent falls back to pageNum 0 when URL does not end with .lesson/<digits>', () => {
    // Change URL to a form that should not match the pageNum regex
    Object.defineProperty(global.document, 'URL', {
      value: 'https://example.com/start.lesson-extra',
      configurable: true,
    });

    const model = new LessonContentModel();

    // Act
    model.setContent('<html>content</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('https://example.com/start.lesson-extra');
    expect(model.get('pageNum')).toBe(0);
  });
});
