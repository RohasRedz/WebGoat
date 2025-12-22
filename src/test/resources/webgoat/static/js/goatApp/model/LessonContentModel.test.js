// File path (derived conceptually from src/main -> src/test): src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js
// NOTE: Adjust import/require paths as needed for the actual project layout.

const { JSDOM } = require('jsdom');

// TODO: Adjust this path based on your module bundler / AMD loader setup.
// For pure Jest without AMD, you may need to refactor the module or use a shim.
jest.mock('backbone', () => {
  const original = jest.requireActual('backbone');
  return {
    ...original,
    Model: class MockModel {
      constructor() {
        this.attributes = {};
      }
      set(key, value) {
        if (typeof key === 'object') {
          Object.assign(this.attributes, key);
        } else {
          this.attributes[key] = value;
        }
      }
      get(key) {
        return this.attributes[key];
      }
      trigger() {
        // no-op for testing
      }
    }
  };
});

jest.mock('goatApp/model/HTMLContentModel', () => {
  // Provide a minimal extension mechanism compatible with the original code
  class HTMLContentModel {
    constructor() {
      this.attributes = {};
    }
    set(key, value) {
      if (typeof key === 'object') {
        Object.assign(this.attributes, key);
      } else {
        this.attributes[key] = value;
      }
    }
    get(key) {
      return this.attributes[key];
    }
    trigger() {
      // no-op
    }
  }

  HTMLContentModel.extend = function(definition) {
    class Extended extends HTMLContentModel {
      constructor(options) {
        super(options);
        if (typeof definition.initialize === 'function') {
          definition.initialize.call(this, options);
        }
      }
    }
    Object.keys(definition).forEach((k) => {
      if (k !== 'initialize') {
        Extended.prototype[k] = definition[k];
      }
    });
    return Extended;
  };

  return HTMLContentModel;
});

// Because the original file is defined as an AMD module, we simulate its export
// by requiring it via a stub loader. In a real project, adapt this to your build.
//
// eslint-disable-next-line global-require
const LessonContentModelFactory = require('../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');

describe('LessonContentModel regex hardening (delta tests)', () => {
  let LessonContentModel;

  beforeAll(() => {
    // For AMD-style define([...], function(...) { return HTMLContentModel.extend({...}) });
    // our require should return the constructor directly.
    LessonContentModel = LessonContentModelFactory;
  });

  beforeEach(() => {
    // Set up a fake DOM for document.URL usage
    const dom = new JSDOM('<!doctype html><html><body></body></html>', {
      url: 'http://example.com/initial'
    });
    global.window = dom.window;
    global.document = dom.window.document;
  });

  test('setContent derives lessonUrl without greedy regex and normalizes query/fragment', () => {
    const model = new LessonContentModel();

    // This URL includes path, query, and fragment components that previously were
    // processed with a greedy /.lesson.*/ pattern. The new implementation should
    // safely normalize and replace only the trailing .lesson segment.
    global.document = new JSDOM('<!doctype html><html><body></body></html>', {
      url: 'http://example.com/path/to/lesson.lesson/1234?page=2#section'
    }).window.document;

    model.setContent('<html></html>', true);

    const lessonUrl = model.get('lessonUrl');
    const pageNum = model.get('pageNum');

    // The new regex should strip trailing path after ".lesson" and remove query/fragment.
    expect(lessonUrl).toBe('http://example.com/path/to/lesson.lesson');

    // Page number extraction should use the anchored, non-greedy regex and still work.
    expect(pageNum).toBe('1234');
  });

  test('setContent sets pageNum to 0 when URL does not match expected pattern', () => {
    const model = new LessonContentModel();

    // URL without a trailing ".lesson/<digits>" pattern
    global.document = new JSDOM('<!doctype html><html><body></body></html>', {
      url: 'http://example.com/path/to/lesson.html'
    }).window.document;

    model.setContent('<html></html>', true);

    expect(model.get('pageNum')).toBe(0);
    // This confirms the safer regex still preserves the fallback behavior and
    // does not mis-parse non-matching URLs.
  });
});
