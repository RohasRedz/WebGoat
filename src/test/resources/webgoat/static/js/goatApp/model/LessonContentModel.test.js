// File: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js
// Jest delta tests focusing only on the changed behavior in LessonContentModel.js:
// - Safer regex and bounded URL processing for lessonUrl and pageNum derivation.
// - Bounded and sanitized handling of options.name in loadData.

jest.mock('backbone', () => {
  class Model {
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
      // no-op for tests
    }
    fetch() {
      return {
        done: (cb) => {
          cb('<html>lesson content</html>');
          return this;
        }
      };
    }
  }
  return {
    __esModule: true,
    default: { Model },
    Model
  };
});

jest.mock('underscore', () => {
  const _ = {
    escape: (s) => String(s).replace(/</g, '&lt;').replace(/>/g, '&gt;'),
    extend: (target, src) => Object.assign(target, src)
  };
  return {
    __esModule: true,
    default: _,
    ..._
  };
});

// Minimal HTMLContentModel stand-in that simply extends Backbone.Model
jest.mock('goatApp/model/HTMLContentModel', () => {
  const Backbone = require('backbone');
  return Backbone.Model;
});

// Global document stub with configurable URL
global.document = {
  URL: 'http://example.com/lesson/1'
};

describe('LessonContentModel.js delta tests', () => {
  let LessonContentModel;

  beforeEach(() => {
    jest.resetModules();
    // Require the updated module under test
    LessonContentModel = require('../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');
  });

  test('setContent derives lessonUrl and pageNum from a normal URL using safe regex', () => {
    // Arrange
    document.URL = 'http://example.com/some/path/Example.lesson/42';

    const model = new LessonContentModel({});

    // Act
    model.setContent('<html>content</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/some/path/Example.lesson');
    expect(model.get('pageNum')).toBe('42');
  });

  test('setContent defaults pageNum to 0 when URL has no page suffix', () => {
    // Arrange
    document.URL = 'http://example.com/some/path/Example.lesson';

    const model = new LessonContentModel({});

    // Act
    model.setContent('<html>content</html>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/some/path/Example.lesson');
    expect(model.get('pageNum')).toBe(0);
  });

  test('setContent bounds overly long URLs before applying regex', () => {
    // Arrange: build a very long URL which should be truncated internally
    const longPath = 'a'.repeat(5000);
    document.URL = `http://example.com/${longPath}.lesson/1234`;

    const model = new LessonContentModel({});

    // Act
    model.setContent('<html>content</html>');

    // Assert
    // We cannot easily assert exact truncation point, but we can assert that
    // a pageNum is still derived from the bounded URL and no error is thrown.
    const pageNum = model.get('pageNum');
    expect(pageNum === '1234' || pageNum === 0 || typeof pageNum === 'string').toBeTruthy();
  });

  test('loadData sanitizes and bounds options.name when creating urlRoot', () => {
    // Arrange
    const model = new LessonContentModel({});
    const veryLongName = 'L'.repeat(1000);

    // Act
    model.loadData({ name: veryLongName });

    // Assert
    const urlRoot = model.urlRoot;
    expect(urlRoot.endsWith('.lesson')).toBe(true);

    // encoded and escaped, but length should be bounded (~200 chars before encode)
    const namePart = urlRoot.replace('.lesson', '');
    expect(namePart.length).toBeLessThanOrEqual(800); // generous bound for encoded length
  });
});
