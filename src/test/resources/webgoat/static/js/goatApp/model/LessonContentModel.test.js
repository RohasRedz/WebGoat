// Derived test path: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

const jsdom = require('jsdom');
const { JSDOM } = jsdom;

// Minimal AMD loader shim for the test
global.define = function (deps, factory) {
  const jquery = require('jquery');
  const _ = require('underscore');
  const Backbone = require('backbone');
  const HTMLContentModel = Backbone.Model.extend({});
  module.exports = factory(jquery, _, Backbone, HTMLContentModel);
};

require('../../../../../main/resources/webgoat/static/js/goatApp/model/LessonContentModel');

describe('LessonContentModel delta tests', () => {
  let LessonContentModel;

  beforeAll(() => {
    LessonContentModel = module.exports;
  });

  test('loadData should build urlRoot using encodeURIComponent without complex regex', () => {
    const model = new LessonContentModel();
    const name = 'Some Lesson/../?weird';

    model.loadData({ name });

    // urlRoot must be encoded; we only check that raw name is not present
    expect(model.urlRoot).toContain('.lesson');
    expect(model.urlRoot).not.toContain(name);
  });

  test('setContent should derive lessonUrl and numeric pageNum without catastrophic regex', () => {
    const dom = new JSDOM(`<!DOCTYPE html><p>Hello</p>`, {
      url: 'https://example.com/lesson/Some.lesson/123'
    });
    global.document = dom.window.document;

    const model = new LessonContentModel();
    const content = '<div>content</div>';
    const listener = jest.fn();
    model.on('content:loaded', listener);

    model.setContent(content, true);

    expect(model.get('lessonUrl')).toBe('https://example.com/lesson/Some.lesson');
    expect(model.get('pageNum')).toBe(123);
    expect(listener).toHaveBeenCalledWith(model, true);
  });

  test('setContent should default pageNum to 0 when URL has no numeric segment', () => {
    const dom = new JSDOM(`<!DOCTYPE html><p>Hello</p>`, {
      url: 'https://example.com/lesson/Some.lesson'
    });
    global.document = dom.window.document;

    const model = new LessonContentModel();
    model.setContent('<div>content</div>', false);

    expect(model.get('pageNum')).toBe(0);
  });
});
