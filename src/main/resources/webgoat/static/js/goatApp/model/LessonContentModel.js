define(
  ['jquery', 'underscore', 'backbone', 'goatApp/model/HTMLContentModel'],
  function ($, _, Backbone, HTMLContentModel) {
    return HTMLContentModel.extend({
      urlRoot: null,
      defaults: {
        items: null,
        selectedItem: null
      },

      initialize: function (options) {
        // no-op
      },

      loadData: function (options) {
        this.urlRoot = _.escape(encodeURIComponent(options.name)) + '.lesson';
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

        // Precompile regular expressions to avoid recreating them on every call
        var lessonUrlPattern = /\.lesson.*/;
        var pageNumPattern = /.*\.lesson\/(\d{1,4})$/;

        // Use the precompiled regex for URL normalization
        this.set('lessonUrl', document.URL.replace(lessonUrlPattern, '.lesson'));

        // Use the precompiled regex for page number extraction
        if (pageNumPattern.test(document.URL)) {
          this.set('pageNum', document.URL.replace(pageNumPattern, '$1'));
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
      }
    });
  }
);
