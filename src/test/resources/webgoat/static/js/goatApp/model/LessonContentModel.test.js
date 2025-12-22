const jsdom = require('jsdom');
const { JSDOM } = jsdom;

// The module is defined via AMD (define). For unit testing, we can approximate by requiring it
// through a test harness or by directly loading the file and evaluating define callbacks.
// Here we simulate AMD's define minimally for the purpose of delta testing.

describe('LessonContentModel - delta tests for URL parsing behavior', () => {
  let LessonContentModel;
  let HTMLContentModelMock;
  let modelInstance;
  let dom;

  beforeEach(() => {
    // Setup DOM
    dom = new JSDOM(``, { url: 'http://localhost' });
    global.window = dom.window;
    global.document = dom.window.document;

    // Minimal Backbone + HTMLContentModel mocks
    const Backbone = {
      Model: class {
        constructor() {
          this.attributes = {};
        }
        set(key, value) {
          this.attributes[key] = value;
        }
        get(key) {
          return this.attributes[key];
        }
        trigger() {
          // no-op for delta test
        }
        fetch() {
          return Promise.resolve();
        }
      },
      ModelPrototype: {}
    };

    HTMLContentModelMock = class extends Backbone.Model {};

    // Simulate AMD define environment
    global.define = function (deps, factory) {
      const $ = {}; // jQuery not needed for this delta test
      const _ = {
        escape: (v) => v
      };
      LessonContentModel = factory($, _, Backbone, HTMLContentModelMock);
    };
    global.define.amd = true;

    // Load the module under test
    // TODO: Adjust relative path if project structure differs.
    require('../../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');

    // Create instance
    modelInstance = new LessonContentModel();
  });

  afterEach(() => {
    delete global.window;
    delete global.document;
    delete global.define;
    LessonContentModel = null;
    modelInstance = null;
  });

  test('setContent should derive lessonUrl and pageNum=0 when URL has no page number', () => {
    // Arrange
    const url = 'http://example.com/path/to/lesson/Intro.lesson';
    dom.reconfigure({ url });
    const content = '<html>content</html>';

    // Act
    modelInstance.setContent(content);

    // Assert
    expect(modelInstance.get('lessonUrl')).toBe(
      'http://example.com/path/to/lesson/Intro.lesson'
    );
    expect(modelInstance.get('pageNum')).toBe(0);
  });

  test('setContent should derive lessonUrl and numeric pageNum when URL ends with .lesson/<pageNum>', () => {
    // Arrange
    const url = 'http://example.com/path/to/lesson/Intro.lesson/12';
    dom.reconfigure({ url });
    const content = '<html>content</html>';

    // Act
    modelInstance.setContent(content);

    // Assert
    expect(modelInstance.get('lessonUrl')).toBe(
      'http://example.com/path/to/lesson/Intro.lesson'
    );
    expect(modelInstance.get('pageNum')).toBe(12);
  });

  test('setContent should set pageNum to 0 when trailing segment is non-numeric', () => {
    // Arrange
    const url = 'http://example.com/path/to/lesson/Intro.lesson/not-a-number';
    dom.reconfigure({ url });
    const content = '<html>content</html>';

    // Act
    modelInstance.setContent(content);

    // Assert
    expect(modelInstance.get('lessonUrl')).toBe(
      'http://example.com/path/to/lesson/Intro.lesson'
    );
    expect(modelInstance.get('pageNum')).toBe(0);
  });

  test('setContent should handle URLs without .lesson gracefully', () => {
    // Arrange
    const url = 'http://example.com/path/to/other/resource/42';
    dom.reconfigure({ url });
    const content = '<html>content</html>';

    // Act
    modelInstance.setContent(content);

    // Assert: when .lesson is absent, lessonUrl should mirror full URL, pageNum parsed if numeric
    expect(modelInstance.get('lessonUrl')).toBe(url);
    expect(modelInstance.get('pageNum')).toBe(42);
  });
});
