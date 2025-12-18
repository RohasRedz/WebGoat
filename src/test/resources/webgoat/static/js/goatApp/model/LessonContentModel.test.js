// Delta tests for LessonContentModel.js regex hardening.
// These tests focus only on the changed behavior: precompiled regex usage for
// lessonUrl and pageNum derivation. We treat the module as AMD but stub a minimal
// AMD environment and its dependencies.
// TODO: If your test harness already wires AMD/RequireJS, replace the manual define/require stubs.

const path = require('path');

describe('LessonContentModel.js delta tests - regex safety and behavior', () => {
  let originalDefine;
  let capturedFactory;
  let HTMLContentModelMock;
  let ModelCtor;

  beforeEach(() => {
    originalDefine = global.define;

    // Minimal Backbone & underscore stubs for the module under test
    global._ = {
      escape: jest.fn(x => x)
    };
    global.Backbone = {
      Model: function () {},
      ModelPrototype: {
        fetch: jest.fn()
      }
    };
    // HTMLContentModel mock to capture the extended model
    HTMLContentModelMock = function () {};
    HTMLContentModelMock.extend = jest.fn(def => {
      ModelCtor = function () {};
      ModelCtor.prototype = Object.assign({}, def);
      return ModelCtor;
    });

    // Stub AMD define to capture the factory
    global.define = jest.fn((deps, factory) => {
      capturedFactory = factory;
    });

    // Load the module; this will call our stubbed define
    const modulePath = path.resolve(
      __dirname,
      '../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js'
    );
    // eslint-disable-next-line global-require, import/no-dynamic-require
    require(modulePath);

    // Now execute the captured factory to obtain the model constructor
    capturedFactory(
      {}, // jquery (not used in setContent)
      _,  // underscore
      { Model: Backbone.Model, prototype: Backbone.ModelPrototype }, // backbone stub
      HTMLContentModelMock
    );
  });

  afterEach(() => {
    global.define = originalDefine;
    delete global._;
    delete global.Backbone;
    jest.resetModules();
    ModelCtor = null;
  });

  test('setContent should normalize lessonUrl by replacing suffix after ".lesson"', () => {
    // Arrange
    const model = new ModelCtor();
    model.set = jest.fn();
    model.trigger = jest.fn();

    const originalUrl = 'https://example.com/path/to/lesson.lesson/1234?param=value';
    Object.defineProperty(global, 'document', {
      value: { URL: originalUrl },
      configurable: true
    });

    // Act
    model.setContent('<html/>', true);

    // Assert
    // First set call is for content, second for lessonUrl, third for pageNum.
    const setCalls = model.set.mock.calls;
    const lessonUrlCall = setCalls.find(call => call[0] === 'lessonUrl');
    expect(lessonUrlCall).toBeDefined();
    const normalizedUrl = lessonUrlCall[1];

    // After regex replacement, everything after ".lesson" should be stripped.
    expect(normalizedUrl).toBe('https://example.com/path/to/lesson.lesson');
  });

  test('setContent should correctly extract pageNum when URL matches the bounded regex', () => {
    // Arrange
    const model = new ModelCtor();
    model.set = jest.fn();
    model.trigger = jest.fn();

    const urlWithPage = 'http://localhost:8080/WebGoat/attack.lesson/123';
    Object.defineProperty(global, 'document', {
      value: { URL: urlWithPage },
      configurable: true
    });

    // Act
    model.setContent('<html/>', true);

    // Assert
    const setCalls = model.set.mock.calls;
    const pageNumCall = setCalls.find(call => call[0] === 'pageNum');
    expect(pageNumCall).toBeDefined();
    expect(pageNumCall[1]).toBe('123');
  });

  test('setContent should default pageNum to 0 when URL does not match page pattern', () => {
    // Arrange
    const model = new ModelCtor();
    model.set = jest.fn();
    model.trigger = jest.fn();

    const urlWithoutPage = 'http://localhost:8080/WebGoat/attack.lesson';
    Object.defineProperty(global, 'document', {
      value: { URL: urlWithoutPage },
      configurable: true
    });

    // Act
    model.setContent('<html/>', true);

    // Assert
    const pageNumCall = model.set.mock.calls.find(call => call[0] === 'pageNum');
    expect(pageNumCall).toBeDefined();
    expect(pageNumCall[1]).toBe(0);
  });
});
