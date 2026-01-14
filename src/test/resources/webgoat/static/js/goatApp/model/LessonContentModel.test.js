// Derived test path: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

const jsdom = require('jsdom');
const { JSDOM } = jsdom;

// Minimal AMD loader shim to allow requiring the module under test.
// In the real project this would be provided by the build/test setup.
global.define = function (deps, factory) {
  // We only care that the factory returns the model; dependencies are not used in delta tests.
  module.exports = factory({}, {}, function () {}, function () {});
};

require('../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel'); // path relative to this test file

describe('LessonContentModel delta tests', () => {
  // The module exports an HTMLContentModel extension; we only need an instance
  // with set(), trigger() behavior for the changed URL parsing logic.
  const LessonContentModel = module.exports;

  function createModel() {
    const model = new LessonContentModel();
    // Spy on set and trigger to observe behavior
    model.set = jest.fn();
    model.trigger = jest.fn();
    return model;
  }

  function withUrl(url, fn) {
    const dom = new JSDOM(`<!doctype html><html><head></head><body></body></html>`, {
      url
    });
    global.document = dom.window.document;
    return fn();
  }

  test('setContent sets lessonUrl trimmed at first .lesson and pageNum extracted safely', () => {
    const model = createModel();
    const url = 'http://example.com/path/topic.lesson/12?unused=query';

    withUrl(url, () => {
      model.setContent('<html>content</html>');

      // First argument should be 'content', second is the actual value.
      const lessonUrlCall = model.set.mock.calls.find((c) => c[0] === 'lessonUrl');
      const pageNumCall = model.set.mock.calls.find((c) => c[0] === 'pageNum');

      expect(lessonUrlCall).toBeDefined();
      expect(lessonUrlCall[1]).toBe('http://example.com/path/topic.lesson');

      expect(pageNumCall).toBeDefined();
      expect(pageNumCall[1]).toBe(12);
    });
  });

  test('setContent sets pageNum to 0 when URL path after .lesson/ is not purely digits', () => {
    const model = createModel();
    const url = 'http://example.com/path/topic.lesson/not-a-number';

    withUrl(url, () => {
      model.setContent('<html>content</html>');

      const pageNumCall = model.set.mock.calls.find((c) => c[0] === 'pageNum');
      expect(pageNumCall).toBeDefined();
      expect(pageNumCall[1]).toBe(0);
    });
  });

  test('setContent tolerates long or unusual URLs without throwing (ReDoS protection)', () => {
    const model = createModel();
    const longSegment = 'x'.repeat(5000);
    const url = `http://example.com/${longSegment}.lesson/${longSegment}`;

    expect(() =>
      withUrl(url, () => {
        model.setContent('<html>content</html>');
      })
    ).not.toThrow();
  });
});
