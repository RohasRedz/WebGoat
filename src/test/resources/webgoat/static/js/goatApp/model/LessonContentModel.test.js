const { JSDOM } = require('jsdom');
const path = require('path');
const fs = require('fs');

function loadLessonContentModel(scriptUrl) {
  const dom = new JSDOM(`<!DOCTYPE html><html><head></head><body></body></html>`, {
    url: scriptUrl || 'http://localhost/lesson/Intro.lesson/3',
    runScripts: 'dangerously',
    resources: 'usable'
  });

  const { window } = dom;
  global.window = window;
  global.document = window.document;

  // Minimal AMD/Backbone/Underscore environment
  const modules = {};
  function define(deps, factory) {
    const resolvedDeps = deps.map((d) => {
      if (d === 'jquery') return {};
      if (d === 'underscore') return require('underscore');
      if (d === 'backbone') return require('backbone');
      if (d === 'goatApp/model/HTMLContentModel') {
        // Simple Backbone.Model stub to allow extension
        const Backbone = require('backbone');
        return Backbone.Model.extend({});
      }
      // TODO: handle other dependencies if needed
      return {};
    });
    const mod = factory.apply(null, resolvedDeps);
    modules['LessonContentModel'] = mod;
  }
  window.define = define;

  const scriptPath = path.resolve(__dirname, '../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');
  const scriptContent = fs.readFileSync(scriptPath, 'utf8');
  const scriptEl = window.document.createElement('script');
  scriptEl.textContent = scriptContent;
  window.document.head.appendChild(scriptEl);

  return { window, LessonContentModel: modules['LessonContentModel'] };
}

describe('LessonContentModel.js delta tests', () => {
  beforeEach(() => {
    jest.resetModules();
    jest.clearAllMocks();
    delete global.window;
    delete global.document;
  });

  test('hardened regex normalizes lessonUrl and extracts pageNum as before', () => {
    const url = 'http://example.com/path/Intro.lesson/123?foo=bar';
    const { LessonContentModel } = loadLessonContentModel(url);
    const model = new LessonContentModel();

    // Act: call setContent which applies the regex logic
    model.setContent('<html>some content</html>');

    // Assert: lessonUrl ends with ".lesson" without extra path/query
    const lessonUrl = model.get('lessonUrl');
    expect(lessonUrl).toBe('http://example.com/path/Intro.lesson');

    // Assert: pageNum is the trailing numeric segment
    const pageNum = model.get('pageNum');
    expect(pageNum).toBe('123');
  });

  test('hardened regex sets pageNum to 0 when no page suffix is present', () => {
    const url = 'http://example.com/path/Intro.lesson';
    const { LessonContentModel } = loadLessonContentModel(url);
    const model = new LessonContentModel();

    model.setContent('<html>some content</html>');

    const lessonUrl = model.get('lessonUrl');
    expect(lessonUrl).toBe('http://example.com/path/Intro.lesson');
    expect(model.get('pageNum')).toBe(0);
  });

  test('hardened regex handles long URLs without catastrophic backtracking', () => {
    // Construct a very long URL to exercise the regex safely
    const longPath = 'a'.repeat(5000);
    const url = `http://example.com/${longPath}/Intro.lesson/99`;
    const { LessonContentModel } = loadLessonContentModel(url);
    const model = new LessonContentModel();

    // If regex were still catastrophic, this could hang; Jest will time out.
    model.setContent('<html>some content</html>');

    expect(model.get('lessonUrl')).toBe(`http://example.com/${longPath}/Intro.lesson`);
    expect(model.get('pageNum')).toBe('99');
  });
});
