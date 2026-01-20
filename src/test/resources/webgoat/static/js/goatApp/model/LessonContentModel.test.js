import $ from 'jquery';
import _ from 'underscore';
import Backbone from 'backbone';
import HTMLContentModel from 'goatApp/model/HTMLContentModel';

describe('LessonContentModel delta tests', () => {
  const LessonContentModel = HTMLContentModel.extend({
    urlRoot: null,
    defaults: {
      items: null,
      selectedItem: null,
    },

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

      const url = document.URL || '';
      this.set('lessonUrl', url.replace(/\.lesson(?:\/.*)?$/, '.lesson'));

      let pageNum = 0;
      const lastSlashIndex = url.lastIndexOf('/');
      if (lastSlashIndex !== -1 && lastSlashIndex + 1 < url.length) {
        const tail = url.substring(lastSlashIndex + 1);
        const pageMatch = /^(\d{1,4})$/.exec(tail);
        if (pageMatch) {
          pageNum = parseInt(pageMatch[1], 10);
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

  test('setContent extracts lessonUrl and numeric pageNum safely', () => {
    const model = new LessonContentModel();
    const triggerSpy = jest.spyOn(model, 'trigger');
    Object.defineProperty(window, 'document', {
      value: { URL: 'http://example.com/lesson/attack.lesson/1234' },
      configurable: true,
    });

    model.setContent('<h1>content</h1>', true);

    expect(model.get('lessonUrl')).toBe(
      'http://example.com/lesson/attack.lesson'
    );
    expect(model.get('pageNum')).toBe(1234);
    expect(triggerSpy).toHaveBeenCalledWith('content:loaded', model, true);
  });

  test('setContent falls back to pageNum 0 when URL tail is not numeric', () => {
    const model = new LessonContentModel();
    Object.defineProperty(window, 'document', {
      value: { URL: 'http://example.com/lesson/attack.lesson/not-a-number' },
      configurable: true,
    });

    model.setContent('<h1>content</h1>', false);

    expect(model.get('pageNum')).toBe(0);
  });
});
