const Backbone = require('backbone');
const _ = require('underscore');

const HTMLContentModel = Backbone.Model.extend({});

function createLessonContentModelModule() {
  return HTMLContentModel.extend({
    urlRoot: null,
    defaults: {
      items: null,
      selectedItem: null,
    },

    initialize: function () {},

    loadData: function (options) {
      this.urlRoot = encodeURIComponent(options.name) + '.lesson';
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

      const currentUrl = global.document.URL;
      let lessonUrl = currentUrl;
      const lessonIndex = currentUrl.indexOf('.lesson');
      if (lessonIndex !== -1) {
        lessonUrl = currentUrl.substring(0, lessonIndex + '.lesson'.length);
      }
      this.set('lessonUrl', lessonUrl);

      let pageNum = 0;
      const pageMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
      if (pageMatch && pageMatch[1]) {
        const parsed = parseInt(pageMatch[1], 10);
        if (!isNaN(parsed)) {
          pageNum = parsed;
        }
      }
      this.set('pageNum', pageNum);

      this.trigger('content:loaded', this, loadHelps);
    },

    fetch: function (options) {
      options = options || {};
      return Backbone.Model.prototype.fetch.call(
        this,
        _.extend({ dataType: 'html' }, options)
      );
    },
  });
}

describe('LessonContentModel URL handling (delta tests)', () => {
  let LessonContentModel;
  let originalDocument;

  beforeEach(() => {
    LessonContentModel = createLessonContentModelModule();
    originalDocument = global.document;
    global.document = { URL: '' };
  });

  afterEach(() => {
    global.document = originalDocument;
  });

  test('setContent derives lessonUrl by trimming after .lesson and parses pageNum', () => {
    global.document.URL =
      'http://example.com/WebGoat.lesson/12?foo=bar#section';
    const model = new LessonContentModel();

    model.setContent('<html>dummy</html>');

    expect(model.get('lessonUrl')).toBe(
      'http://example.com/WebGoat.lesson'
    );
    expect(model.get('pageNum')).toBe(12);
  });

  test('setContent defaults pageNum to 0 when URL has no page suffix', () => {
    global.document.URL = 'http://example.com/WebGoat.lesson';
    const model = new LessonContentModel();

    model.setContent('<html>dummy</html>');

    expect(model.get('lessonUrl')).toBe(
      'http://example.com/WebGoat.lesson'
    );
    expect(model.get('pageNum')).toBe(0);
  });
});
