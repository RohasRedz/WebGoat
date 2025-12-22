const { JSDOM } = require('jsdom');

jest.mock('backbone', () => {
  const Backbone = {
    Model: function () {},
  };
  Backbone.Model.prototype = {
    fetch: jest.fn(function (options) {
      return {
        done: (cb) => {
          cb('<html></html>');
        },
      };
    }),
  };
  return Backbone;
});

jest.mock('goatApp/model/HTMLContentModel', () => {
  const Backbone = require('backbone');
  function HTMLContentModel() {}
  HTMLContentModel.prototype = Object.create(Backbone.Model.prototype);
  HTMLContentModel.extend = function (props) {
    function Child() {
      this.attributes = {};
      if (props.initialize) {
        props.initialize.apply(this, arguments);
      }
    }
    Child.prototype = Object.create(HTMLContentModel.prototype);
    Object.assign(Child.prototype, props, {
      set: function (key, value) {
        this.attributes[key] = value;
      },
      get: function (key) {
        return this.attributes[key];
      },
      trigger: jest.fn(),
    });
    return Child;
  };
  return HTMLContentModel;
});

const _ = require('underscore');
const Backbone = require('backbone');
const HTMLContentModel = require('goatApp/model/HTMLContentModel');

function createLessonContentModelModule() {
  return (function ($, _, Backbone, HTMLContentModel) {
    return HTMLContentModel.extend({
      urlRoot: null,
      defaults: {
        items: null,
        selectedItem: null,
      },

      initialize: function (options) {},

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

        const currentUrl = document.URL;

        this.set('lessonUrl', currentUrl.replace(/\.lesson$/, '.lesson'));

        const pageMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
        if (pageMatch) {
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
          _.extend({ dataType: 'html' }, options)
        );
      },
    });
  })(null, _, Backbone, HTMLContentModel);
}

describe('LessonContentModel delta tests', () => {
  let LessonContentModel;

  beforeEach(() => {
    LessonContentModel = createLessonContentModelModule();
  });

  test('setContent computes lessonUrl and pageNum for URL ending with .lesson', () => {
    const dom = new JSDOM('<!doctype html><html><body></body></html>', {
      url: 'http://example/app.lesson',
    });
    global.document = dom.window.document;

    const model = new LessonContentModel();

    model.setContent('<html>content</html>');

    expect(model.get('lessonUrl')).toBe('http://example/app.lesson');
    expect(model.get('pageNum')).toBe(0);
  });

  test('setContent computes lessonUrl and pageNum for URL ending with .lesson/<digits>', () => {
    const dom = new JSDOM('<!doctype html><html><body></body></html>', {
      url: 'http://example/app.lesson/12',
    });
    global.document = dom.window.document;

    const model = new LessonContentModel();

    model.setContent('<html>content</html>');

    expect(model.get('lessonUrl')).toBe('http://example/app.lesson/12'.replace(/\.lesson$/, '.lesson'));
    expect(model.get('pageNum')).toBe('12');
  });
});
