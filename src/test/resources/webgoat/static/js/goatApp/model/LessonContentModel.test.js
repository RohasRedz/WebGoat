/**
 * Delta tests for LessonContentModel.js focusing only on:
 * - setContent() computing lessonUrl using the safer regex pattern.
 * - setContent() computing pageNum as expected for various URLs.
 */

jest.mock('backbone', () => {
  const Backbone = {
    Model: function () {},
  };
  Backbone.Model.prototype.fetch = jest.fn(function (options) {
    return {
      done: (cb) => {
        cb('<html></html>');
        return { done: jest.fn() };
      },
    };
  });
  Backbone.Model.prototype.set = jest.fn();
  Backbone.Model.prototype.trigger = jest.fn();
  return Backbone;
});

jest.mock('underscore', () => {
  const _ = function () {};
  _.extend = Object.assign;
  _.escape = (s) => s;
  return _;
});

const Backbone = require('backbone');
const _ = require('underscore');

describe('LessonContentModel delta tests', () => {
  let LessonContentModel;
  let originalDocument;

  beforeEach(() => {
    jest.resetModules();
    originalDocument = global.document;
    global.document = { URL: '' };

    // Require the AMD-style module via a shim; in real tests, a loader like RequireJS or webpack would be used.
    // TODO: Adapt require path/resolution to actual test environment if needed.
    LessonContentModel = require('../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');
  });

  afterEach(() => {
    global.document = originalDocument;
  });

  function createModelInstance() {
    // HTMLContentModel.extend(...) returns a Backbone model-like constructor
    return new LessonContentModel();
  }

  test('setContent computes lessonUrl ending with .lesson for a simple URL', () => {
    // Arrange
    global.document.URL = 'http://host/lesson/SqlInjection.lesson/1';
    const model = createModelInstance();
    model.set = jest.fn();
    model.trigger = jest.fn();

    // Act
    model.setContent('<html/>');

    // Assert
    const setCalls = model.set.mock.calls;
    const lessonUrlCall = setCalls.find(([key]) => key === 'lessonUrl');
    expect(lessonUrlCall).toBeDefined();
    const lessonUrl = lessonUrlCall[1];
    expect(lessonUrl).toBe('http://host/lesson/SqlInjection.lesson');
  });

  test('setContent sets pageNum from URL when pattern .lesson/<digits> is present', () => {
    // Arrange
    global.document.URL = 'https://example.com/Path/Some.lesson/123';
    const model = createModelInstance();
    model.set = jest.fn();
    model.trigger = jest.fn();

    // Act
    model.setContent('<html/>');

    // Assert
    const setCalls = model.set.mock.calls;
    const pageNumCall = setCalls.find(([key]) => key === 'pageNum');
    expect(pageNumCall).toBeDefined();
    const pageNum = pageNumCall[1];
    expect(pageNum).toBe('123');
  });

  test('setContent sets pageNum to 0 when URL does not end with .lesson/<digits>', () => {
    // Arrange
    global.document.URL = 'https://example.com/Path/Some.lesson-extra';
    const model = createModelInstance();
    model.set = jest.fn();
    model.trigger = jest.fn();

    // Act
    model.setContent('<html/>');

    // Assert
    const setCalls = model.set.mock.calls;
    const pageNumCall = setCalls.find(([key]) => key === 'pageNum');
    expect(pageNumCall).toBeDefined();
    const pageNum = pageNumCall[1];
    expect(pageNum).toBe(0);
  });
});
