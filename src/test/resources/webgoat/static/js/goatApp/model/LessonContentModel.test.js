// NOTE: This test assumes an AMD/RequireJS environment is configured for tests.
// It focuses only on the changed regular expression behavior in setContent().

const jsdom = require('jsdom');
const { JSDOM } = jsdom;

describe('LessonContentModel  delta regex behavior', () => {
  let LessonContentModel;
  let model;

  beforeEach((done) => {
    // Set up a minimal DOM environment
    const dom = new JSDOM('<!doctype html><html><body></body></html>', {
      url: 'http://localhost/WebGoat.lesson/12'
    });
    global.window = dom.window;
    global.document = dom.window.document;

    // Mock Backbone & dependencies for AMD module
    const Backbone = require('backbone');
    const _ = require('underscore');
    const $ = require('jquery')(dom.window);

    jest.resetModules();
    jest.doMock('backbone', () => Backbone);
    jest.doMock('underscore', () => _);
    jest.doMock('jquery', () => $);
    jest.doMock('goatApp/model/HTMLContentModel', () =>
      Backbone.Model.extend({
        setContent() {}
      })
    );

    // Load AMD module via requirejs-like loader; in a real test harness, this would be wired
    // by the build/test tooling. Here we require the built module path directly.
    // eslint-disable-next-line global-require
    LessonContentModel = require('../../../../../webgoat/static/js/goatApp/model/LessonContentModel.js');
    model = new LessonContentModel();
    done();
  });

  test('setContent normalizes lessonUrl and pageNum using hardened regex', () => {
    // Arrange
    const content = '<h1>Test Lesson</h1>';
    const loadHelps = true;

    // Act
    model.setContent(content, loadHelps);

    // Assert
    // The new regex `/\.lesson(?:\/.*)?$/` should normalize any trailing path
    // so lessonUrl ends with ".lesson" only.
    expect(model.get('lessonUrl')).toBe('http://localhost/WebGoat.lesson');

    // The tightened pageNum regex `/^.*\.lesson\/(\d{1,4})$/` still extracts the page number.
    expect(model.get('pageNum')).toBe('12');
  });

  test('setContent falls back to pageNum 0 when URL does not match pattern', () => {
    // Arrange
    const dom = new JSDOM('<!doctype html><html><body></body></html>', {
      url: 'http://localhost/WebGoat.lesson'
    });
    global.window = dom.window;
    global.document = dom.window.document;

    const content = '<h1>No Page Number</h1>';

    // Act
    model.setContent(content);

    // Assert
    expect(model.get('pageNum')).toBe(0);
  });
});
