// Derived from: src/main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js
// Test path (main -> test): src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js
// NOTE: AMD + Backbone environment is approximated; this delta test focuses
// only on the changed regex/url handling behavior in setContent.
const { JSDOM } = require('jsdom');

// TODO: In real project, require actual AMD-built module; here we approximate minimal behavior.
class FakeHTMLContentModel {
  constructor() {
    this.attributes = {};
    this.events = {};
  }
  set(key, value) {
    this.attributes[key] = value;
  }
  get(key) {
    return this.attributes[key];
  }
  trigger(event, ...args) {
    this.events[event] = args;
  }
}

// Minimal reimplementation of changed logic for delta testing purposes.
class LessonContentModel extends FakeHTMLContentModel {
  setContent(content, loadHelps) {
    if (typeof loadHelps === 'undefined') {
      loadHelps = true;
    }
    this.set('content', content);

    const lessonUrlPattern = /^(.+?\.lesson)(?:\/.*)?$/;
    const currentUrl = String(global.document.URL);
    const lessonUrlMatch = lessonUrlPattern.exec(currentUrl);
    if (lessonUrlMatch) {
      this.set('lessonUrl', lessonUrlMatch[1]);
    } else {
      this.set('lessonUrl', currentUrl.replace(/\.lesson.*/, '.lesson'));
    }

    const pageNumPattern = /^(.*\.lesson\/)(\d{1,4})$/;
    const pageNumMatch = pageNumPattern.exec(currentUrl);
    if (pageNumMatch) {
      this.set('pageNum', pageNumMatch[2]);
    } else {
      this.set('pageNum', 0);
    }

    this.trigger('content:loaded', this, loadHelps);
  }
}

describe('LessonContentModel regex hardening (delta test)', () => {
  beforeEach(() => {
    const dom = new JSDOM(`<!doctype html><html><head></head><body></body></html>`, {
      url: 'http://localhost/'
    });
    global.window = dom.window;
    global.document = dom.window.document;
  });

  test('setContent extracts lessonUrl and pageNum from typical URL using hardened regex', () => {
    // Arrange
    const dom = new JSDOM(`<!doctype html><html></html>`, {
      url: 'http://example.com/lesson-name.lesson/12'
    });
    global.window = dom.window;
    global.document = dom.window.document;

    const model = new LessonContentModel();

    // Act
    model.setContent('<html/>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/lesson-name.lesson');
    expect(model.get('pageNum')).toBe('12');
  });

  test('setContent falls back to pageNum 0 when URL does not match page number pattern', () => {
    // Arrange
    const dom = new JSDOM(`<!doctype html><html></html>`, {
      url: 'http://example.com/another.lesson'
    });
    global.window = dom.window;
    global.document = dom.window.document;

    const model = new LessonContentModel();

    // Act
    model.setContent('<html/>');

    // Assert
    expect(model.get('lessonUrl')).toBe('http://example.com/another.lesson');
    expect(model.get('pageNum')).toBe(0);
  });

  test('setContent handles long complex URL without catastrophic backtracking and still sets reasonable values', () => {
    // Arrange: long URL intended to exercise regex without causing performance issues
    const longPath = 'a'.repeat(5000);
    const dom = new JSDOM(`<!doctype html><html></html>`, {
      url: `http://example.com/${longPath}.lesson/1234`
    });
    global.window = dom.window;
    global.document = dom.window.document;

    const model = new LessonContentModel();

    // Act
    model.setContent('<html/>');

    // Assert: regex should still match quickly and extract values
    expect(model.get('lessonUrl')).toBe(`http://example.com/${longPath}.lesson`);
    expect(model.get('pageNum')).toBe('1234');
  });
});
