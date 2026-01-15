define(
  ['jquery', 'underscore', 'backbone', 'goatApp/model/HTMLContentModel'],
  function ($, _, Backbone, HTMLContentModel) {
    return HTMLContentModel.extend({
      urlRoot: null,
      defaults: {
        items: null,
        selectedItem: null,
      },

      initialize: function (options) {},

      loadData: function (options) {
        this.urlRoot = encodeURIComponent(options.name) + '.lesson';
        var self = this;
        this.fetch().done(function (data) {
          self.setContent(data);
        });
      },

      setContent: function (content, loadHelps) {
        if (typeof loadHelps === 'undefined') {
          loadHelps = true;
        }
        this.set('content', content);

        var currentUrl = document.URL;
        var lessonUrl = currentUrl;
        var lessonIndex = currentUrl.indexOf('.lesson');
        if (lessonIndex !== -1) {
          lessonUrl = currentUrl.substring(0, lessonIndex + '.lesson'.length);
        }
        this.set('lessonUrl', lessonUrl);

        var pageNum = 0;
        var pageMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
        if (pageMatch && pageMatch[1]) {
          var parsed = parseInt(pageMatch[1], 10);
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
);
