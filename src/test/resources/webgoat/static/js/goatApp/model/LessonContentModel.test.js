// File path assumption based on standard JS test layout:
// src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

// TODO: Adjust require path if test runner/project root differs.
const path = require('path');

// Backbone/underscore/jquery will be stubbed for this delta test.
describe('LessonContentModel.js delta tests', () => {
  let LessonContentModel;
  let modelInstance;
  let HTMLContentModelMock;
  let BackboneMock;
  let _;

  beforeEach(() => {
    jest.resetModules();

    // Minimal underscore mock
    _ = {
      escape: (s) => s
    };

    // Minimal Backbone mock with extend and Model.fetch
    BackboneMock = {
      Model: function () {},
      ModelPrototype: {
        fetch: jest.fn(function (options) {
          // Return a thenable stub for simplicity
          return {
            done: function () {}
          };
        })
      }
    };
    BackboneMock.Model.prototype = BackboneMock.ModelPrototype;
    BackboneMock.Model.prototype.fetch = BackboneMock.ModelPrototype.fetch;
    BackboneMock.Model.extend = function (props) {
      function Child() {
        BackboneMock.Model.call(this);
        if (typeof props.initialize === 'function') {
          props.initialize.apply(this, arguments);
        }
      }
      Child.prototype = Object.create(BackboneMock.Model.prototype);
      Child.prototype.constructor = Child;
      Object.assign(Child.prototype, props);
      // simple set/get/trigger shim
      Child.prototype._data = {};
      Child.prototype.set = function (k, v) { this._data[k] = v; };
      Child.prototype.get = function (k) { return this._data[k]; };
      Child.prototype.trigger = function () {};
      return Child;
    };

    HTMLContentModelMock = {
      extend: BackboneMock.Model.extend.bind(BackboneMock.Model)
    };

    jest.mock('jquery', () => ({}), { virtual: true });
    jest.mock('underscore', () => _, { virtual: true });
    jest.mock('backbone', () => BackboneMock, { virtual: true });
    jest.mock('goatApp/model/HTMLContentModel', () => HTMLContentModelMock, { virtual: true });

    const lessonContentModelPath = path.resolve(
      __dirname,
      '../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js'
    );

    // The module is AMD-style with define([...], function(...) { return ... }); .
    // For this delta test, we simulate define so that requiring the file works.
    global.define = function (deps, factory) {
      const $ = {};
      const backbone = BackboneMock;
      const htmlContentModel = HTMLContentModelMock;
      // deps order: ['jquery','underscore','backbone','goatApp/model/HTMLContentModel']
      LessonContentModel = factory($, _, backbone, htmlContentModel);
    };

    require(lessonContentModelPath);
    modelInstance = new LessonContentModel();
  });

  test('setContent computes lessonUrl by truncating at .lesson and does not rely on complex regex', () => {
    // Arrange
    global.document = {
      URL: 'http://localhost/WebGoat/lesson/SomeTopic.lesson/3'
    };

    // Act
    modelInstance.setContent('<html/>');

    // Assert
    const lessonUrl = modelInstance.get('lessonUrl');
    expect(lessonUrl).toBe('http://localhost/WebGoat/lesson/SomeTopic.lesson');

    const pageNum = modelInstance.get('pageNum');
    expect(pageNum).toBe(3);
  });

  test('setContent sets pageNum to 0 when URL does not contain a numeric page suffix', () => {
    // Arrange
    global.document = {
      URL: 'http://localhost/WebGoat/lesson/SomeTopic.lesson'
    };

    // Act
    modelInstance.setContent('<html/>');

    // Assert
    const lessonUrl = modelInstance.get('lessonUrl');
    expect(lessonUrl).toBe('http://localhost/WebGoat/lesson/SomeTopic.lesson');

    const pageNum = modelInstance.get('pageNum');
    expect(pageNum).toBe(0);
  });
});
