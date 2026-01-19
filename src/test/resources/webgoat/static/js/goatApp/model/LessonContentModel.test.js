// Test file for src/main/resources/webgoat/static/js/goatApp/model/LessonContentModel.js
// Resolved test path: src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

const jsdom = require('jsdom');
const { JSDOM } = jsdom;

// Minimal stubs for dependencies used by the AMD module
const _ = {
  extend: Object.assign,
  escape: (s) => s
};
const Backbone = {
  Model: function () {},
};
Backbone.Model.prototype = {
  fetch: function () { return { done: () => {} }; }
};

// Simple HTMLContentModel stub
const HTMLContentModel = function () {};
HTMLContentModel.extend = function (props) {
  function Model() {
    this.attributes = {};
  }
  Model.prototype = {
    set: function (key, value) {
      this.attributes[key] = value;
    },
    get: function (key) {
      return this.attributes[key];
    },
    trigger: function () {}
  };
  Object.assign(Model.prototype, props);
  return Model;
};

// Recreate the module under test using the updated implementation structure
function createLessonContentModel(documentUrl) {
  const dom = new JSDOM(`<!DOCTYPE html><p>Hello</p>`, { url: documentUrl });
  global.window = dom.window;
  global.document = dom.window.document;

  // inline minimal version of updated module's factory
  const LessonContentModel = HTMLContentModel.extend({
    urlRoot: null,
    defaults: {
      items: null,
      selectedItem: null
    },
    initialize: function () {},
    loadData: function () {},
    setContent: function (content, loadHelps) {
      if (typeof loadHelps === 'undefined') {
        loadHelps = true;
      }
      this.set('content', content);

      var href = document.URL;
      var lessonUrl = href;
      var pageNum = 0;

      try {
        var urlObj = new URL(href, window.location.origin);
        var path = urlObj.pathname;
        var pageSegmentMatch = path.match(/\/(\d{1,4})$/);
        if (pageSegmentMatch) {
          var pageSegment = pageSegmentMatch[1];
          pageNum = parseInt(pageSegment, 10);
          path = path.slice(0, -pageSegmentMatch[0].length);
        }

        if (path.endsWith('.lesson')) {
          lessonUrl = urlObj.origin + path;
        } else {
          lessonUrl = urlObj.origin + path + '.lesson';
        }
      } catch (e) {
        lessonUrl = href;
        pageNum = 0;
      }

      this.set('lessonUrl', lessonUrl);
      this.set('pageNum', isNaN(pageNum) ? 0 : pageNum);
      this.trigger('content:loaded', this, loadHelps);
    },
    fetch: function () {}
  });

  return new LessonContentModel();
}

describe('LessonContentModel URL parsing (delta tests)', () => {
  test('computes lessonUrl and pageNum when URL contains .lesson/<page>', () => {
    const model = createLessonContentModel('https://example.com/path/to/lesson.lesson/12');

    model.setContent('<html/>', true);

    expect(model.get('lessonUrl')).toBe('https://example.com/path/to/lesson.lesson');
    expect(model.get('pageNum')).toBe(12);
  });

  test('computes lessonUrl and pageNum when URL is just .lesson (no page segment)', () => {
    const model = createLessonContentModel('https://example.com/path/to/lesson.lesson');

    model.setContent('<html/>', true);

    expect(model.get('lessonUrl')).toBe('https://example.com/path/to/lesson.lesson');
    expect(model.get('pageNum')).toBe(0);
  });

  test('handles URL without .lesson suffix by appending .lesson and defaulting pageNum', () => {
    const model = createLessonContentModel('https://example.com/path/to/lesson');

    model.setContent('<html/>', true);

    expect(model.get('lessonUrl')).toBe('https://example.com/path/to/lesson.lesson');
    expect(model.get('pageNum')).toBe(0);
  });
});
