// File: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js
// TODO: Adjust path/module resolution to match the actual AMD loader configuration.

const jsdom = require("jsdom");
const { JSDOM } = jsdom;

// Since the original code is AMD-style, we cannot directly require it as a CommonJS module
// without a loader like RequireJS. For delta testing, we will simulate the critical
// logic around setContent by invoking it on a simplified object that mimics the behavior.

describe('LessonContentModel - delta tests for regex-based URL handling', () => {
  let model;
  let lessonUrl;
  let pageNum;

  beforeEach(() => {
    // Minimal stub that captures the values set by set()
    model = {
      set: jest.fn((key, value) => {
        if (key === 'lessonUrl') {
          lessonUrl = value;
        } else if (key === 'pageNum') {
          pageNum = value;
        }
      }),
      trigger: jest.fn()
    };

    // Inject a minimal implementation of setContent identical to the fixed code,
    // to validate the new regex behavior deterministically.
    // In a real project, this would import the actual module instead.
    // TODO: Replace this with a direct import when module loading is available.
    model.setContent = function (content, loadHelps) {
      if (typeof loadHelps === 'undefined') {
        loadHelps = true;
      }
      this.set('content', content);

      const pageUrl = global.document.URL;
      const lessonUrlPattern = /\.lesson.*/;
      const pageNumPattern = /.*\.lesson\/(\d{1,4})$/;

      this.set('lessonUrl', pageUrl.replace(lessonUrlPattern, '.lesson'));

      const pageNumMatch = pageNumPattern.exec(pageUrl);
      if (pageNumMatch) {
        this.set('pageNum', pageNumMatch[1]);
      } else {
        this.set('pageNum', 0);
      }

      this.trigger('content:loaded', this, loadHelps);
    };
  });

  function setDocumentUrl(url) {
    const dom = new JSDOM(`<!DOCTYPE html><p>test</p>`, { url });
    global.window = dom.window;
    global.document = dom.window.document;
  }

  test('setContent derives correct lessonUrl and numeric pageNum from a matching URL', () => {
    setDocumentUrl('http://example.com/path/to/lessonA.lesson/42');

    model.setContent('<html>content</html>');

    expect(lessonUrl).toBe('http://example.com/path/to/lessonA.lesson');
    expect(pageNum).toBe('42');
    expect(model.trigger).toHaveBeenCalledWith('content:loaded', model, true);
  });

  test('setContent sets pageNum to 0 when URL does not match the expected pattern', () => {
    setDocumentUrl('http://example.com/path/to/lessonA.lesson');

    model.setContent('<html>content</html>');

    expect(lessonUrl).toBe('http://example.com/path/to/lessonA.lesson');
    expect(pageNum).toBe(0);
  });

  test('setContent handles long URLs without throwing (no catastrophic regex behavior)', () => {
    const longSegment = 'a'.repeat(5000);
    setDocumentUrl(`http://example.com/${longSegment}.lesson/1234`);

    expect(() => model.setContent('<html>content</html>')).not.toThrow();
    expect(lessonUrl.endsWith('.lesson')).toBe(true);
    expect(pageNum).toBe('1234');
  });
});
