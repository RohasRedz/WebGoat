const { JSDOM } = require('jsdom');

class StubModel {
  constructor(attrs = {}) {
    this.attributes = { ...attrs };
    this._listeners = {};
  }
  set(key, value) {
    this.attributes[key] = value;
  }
  get(key) {
    return this.attributes[key];
  }
  trigger(eventName, ...args) {
    if (this._listeners[eventName]) {
      this._listeners[eventName].forEach((fn) => fn(...args));
    }
  }
  on(eventName, fn) {
    if (!this._listeners[eventName]) this._listeners[eventName] = [];
    this._listeners[eventName].push(fn);
  }
}

global.define = function (deps, factory) {
  const $ = {};
  const _ = {
    escape: (s) => s,
  };
  const Backbone = {
    Model: StubModel,
  };
  const HTMLContentModel = StubModel;

  module.exports = factory($, _, Backbone, HTMLContentModel);
};

require('../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js');
const LessonContentModel = module.exports;

describe('LessonContentModel delta tests (regex behavior)', () => {
  beforeEach(() => {
    const dom = new JSDOM(`<!DOCTYPE html><p>Test</p>`, {
      url: 'http://localhost/WebGoat.lesson',
    });
    global.window = dom.window;
    global.document = dom.window.document;
  });

  test('setContent normalizes lessonUrl by replacing only the trailing `.lesson` segment', () => {
    const dom = new JSDOM(`<!DOCTYPE html><p>Test</p>`, {
      url: 'http://localhost/app/WebGoat.lesson/123?x=y',
    });
    global.window = dom.window;
    global.document = dom.window.document;

    const model = new LessonContentModel();
    model.setContent('<html>content</html>');

    expect(model.get('lessonUrl')).toBe('http://localhost/app/WebGoat.lesson');
  });

  test('setContent sets pageNum from trailing digits after `.lesson/NNN`', () => {
    const dom = new JSDOM(`<!DOCTYPE html><p>Test</p>`, {
      url: 'http://localhost/app/WebGoat.lesson/42',
    });
    global.window = dom.window;
    global.document = dom.window.document;

    const model = new LessonContentModel();
    model.setContent('<html>content</html>');

    expect(model.get('pageNum')).toBe('42');
  });

  test('setContent defaults pageNum to 0 when URL does not match `.lesson/NNN` pattern', () => {
    const dom = new JSDOM(`<!DOCTYPE html><p>Test</p>`, {
      url: 'http://localhost/app/WebGoat',
    });
    global.window = dom.window;
    global.document = dom.window.document;

    const model = new LessonContentModel();
    model.setContent('<html>content</html>');

    expect(model.get('pageNum')).toBe(0);
  });

  test('setContent emits content:loaded event with loadHelps inferred to true by default', () => {
    const dom = new JSDOM(`<!DOCTYPE html><p>Test</p>`, {
      url: 'http://localhost/app/WebGoat.lesson/10',
    });
    global.window = dom.window;
    global.document = dom.window.document;

    const model = new LessonContentModel();
    const handler = jest.fn();
    model.on('content:loaded', handler);

    model.setContent('<html>content</html>');

    expect(handler).toHaveBeenCalledTimes(1);
    const [self, loadHelps] = handler.mock.calls[0];
    expect(self).toBe(model);
    expect(loadHelps).toBe(true);
  });
});
