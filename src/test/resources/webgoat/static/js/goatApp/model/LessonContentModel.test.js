// File path assumption:
// Source: src/main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js
// Test:   src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

// TODO: Adjust module loader configuration if your test runner requires a different path setup.

/**
 * Delta tests for LessonContentModel focusing on changed URL parsing behavior:
 * - lessonUrl is derived from document.URL by stripping everything after ".lesson".
 * - pageNum is correctly parsed for URLs with and without a page number segment.
 */

jest.mock('backbone', () => {
  const original = jest.requireActual('backbone');
  return {
    ...original,
    Model: class MockModel {
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
        return { done: () => {} };
      }
    }
  };
});

jest.mock('goatApp/model/HTMLContentModel', () => {
  const Backbone = require('backbone');
  return Backbone.Model.extend({
    set: function (key, value) {
      this.attributes = this.attributes || {};
      this.attributes[key] = value;
    },
    get: function (key) {
      return (this.attributes || {})[key];
    },
    trigger: function () {
      // no-op
    },
    fetch: function () {
      return { done: () => {} };
    }
  });
});

const _ = require('underscore');
const Backbone = require('backbone');

// Simulate AMD define wrapper for the module under test
let LessonContentModelFactory;
beforeAll(() => {
  // We reconstruct the AMD module locally for testing
  const moduleFactory = require('../../../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');
  // If the file is AMD-only, it might not export; in that case, we would need a build step.
  // TODO: If this require fails in your environment, wire the module through your AMD loader.
  LessonContentModelFactory = moduleFactory || require('goatApp/model/HTMLContentModel').extend({});
});

describe('LessonContentModel URL parsing (delta tests)', () => {
  let LessonContentModel;

  beforeEach(() => {
    // In many AMD setups, the factory returns the extended model directly.
    LessonContentModel = LessonContentModelFactory.extend
      ? LessonContentModelFactory
      : require('goatApp/model/HTMLContentModel').extend({});
  });

  function createModelAndSetContent(url, loadHelps) {
    const model = new LessonContentModel();
    const originalUrl = global.document && global.document.URL;

    global.document = { URL: url };
    try {
      model.setContent('<html></html>', loadHelps);
    } finally {
      if (originalUrl !== undefined) {
        global.document.URL = originalUrl;
      }
    }
    return model;
  }

  test('sets lessonUrl and pageNum=0 when URL has no page number', () => {
    const url = 'http://example.com/lesson/123.lesson';
    const model = createModelAndSetContent(url);

    expect(model.get('lessonUrl')).toBe('http://example.com/lesson/123.lesson');
    expect(model.get('pageNum')).toBe(0);
  });

  test('parses pageNum when URL matches .lesson/{pageNumber}', () => {
    const url = 'http://example.com/lesson/123.lesson/7';
    const model = createModelAndSetContent(url);

    expect(model.get('lessonUrl')).toBe('http://example.com/lesson/123.lesson');
    expect(model.get('pageNum')).toBe('7');
  });

  test('parses multi-digit pageNum up to 4 digits', () => {
    const url = 'http://example.com/lesson/9999.lesson/1234';
    const model = createModelAndSetContent(url);

    expect(model.get('lessonUrl')).toBe('http://example.com/lesson/9999.lesson');
    expect(model.get('pageNum')).toBe('1234');
  });

  test('sets pageNum=0 when URL does not match expected pattern', () => {
    const url = 'http://example.com/lesson/9999.les/1234';
    const model = createModelAndSetContent(url);

    expect(model.get('lessonUrl')).toBe('http://example.com/lesson/9999.les');
    expect(model.get('pageNum')).toBe(0);
  });
});
