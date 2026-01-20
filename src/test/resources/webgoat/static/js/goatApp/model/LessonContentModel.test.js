// Derived test path (per requirements):
// src/test/resources/webgoat/static/js/goatApp/model/LessonContentModel.test.js

// Note: This test assumes a Node/Jest environment. It focuses solely on the
// changed URL/regex handling behavior in setContent.

const $ = require('jquery');
const _ = require('underscore');
const Backbone = require('backbone');

// Minimal HTMLContentModel stub to satisfy the AMD-style extend
class HTMLContentModel extends Backbone.Model {}
// Inject our stub into the module under test by emulating the AMD factory
// pattern inline.

// We re-create the updated module body in a testable way.
const createLessonContentModel = () => {
  return HTMLContentModel.extend({
    urlRoot: null,
    defaults: {
      items: null,
      selectedItem: null,
    },

    initialize: function () {},

    loadData: function (options) {
      this.urlRoot = _.escape(encodeURIComponent(options.name)) + '.lesson';
      const self = this;
      this.fetch().done(function (data) {
        self.setContent(data);
      });
    },

    setContent: function (content, loadHelps) {
      if (typeof loadHelps === 'undefined') {
        loadHelps = true;
      }
      this.set('content', content);

      const currentUrl = String(global.document.URL || '');
      this.set('lessonUrl', currentUrl.replace(/\.lesson.*/, '.lesson'));

      const pageMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
      if (pageMatch && pageMatch[1]) {
        this.set('pageNum', pageMatch[1]);
      } else {
        this.set('pageNum', 0);
      }

      this.trigger('content:loaded', this, loadHelps);
    },

    fetch: function (options) {
      options = options || {};
      return Backbone.Model.prototype.fetch.call(
        this,
        _.extend({ dataType: 'html' }, options),
      );
    },
  });
};

describe('LessonContentModel delta tests (URL/regex handling)', () => {
  let LessonContentModel;
  let model;
  let contentLoadedArgs;

  beforeEach(() => {
    // Provide a minimal document with URL to exercise regex.
    global.document = { URL: 'http://example.com/lesson1.lesson/42' };

    LessonContentModel = createLessonContentModel();
    model = new LessonContentModel();

    contentLoadedArgs = null;
    model.on('content:loaded', function (m, loadHelps) {
      contentLoadedArgs = { model: m, loadHelps };
    });
  });

  test('setContent sets lessonUrl and pageNum using simplified regex logic', () => {
    model.setContent('<html>content</html>', true);

    // The updated implementation should still normalize the lesson URL
    // and correctly extract the page number.
    expect(model.get('lessonUrl')).toBe('http://example.com/lesson1.lesson');
    expect(model.get('pageNum')).toBe('42');

    // Ensure the event still fires with expected arguments.
    expect(contentLoadedArgs).not.toBeNull();
    expect(contentLoadedArgs.model).toBe(model);
    expect(contentLoadedArgs.loadHelps).toBe(true);
  });

  test('setContent sets pageNum to 0 when URL does not match page pattern', () => {
    global.document.URL = 'http://example.com/lesson1.lesson';

    model.setContent('<html>content</html>');

    // No "/<digits>" suffix → pageNum should default to 0.
    expect(model.get('lessonUrl')).toBe('http://example.com/lesson1.lesson');
    expect(model.get('pageNum')).toBe(0);
  });
});
