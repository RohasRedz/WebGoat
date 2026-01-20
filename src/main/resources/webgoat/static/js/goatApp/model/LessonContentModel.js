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
      },

      loadData: function (options) {
        // Sanitize and safely encode the lesson name before using it in the URL
        var rawName = options && typeof options.name === 'string' ? options.name : '';
        // Remove any control characters or unsafe URL characters
        var safeName = rawName.replace(/[^\w\-\.]/g, '');
        this.urlRoot = encodeURIComponent(safeName) + '.lesson';

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

        // Use location.href instead of document.URL and avoid overly complex regexes
        var currentUrl = String(window.location.href || '');
        this.set('lessonUrl', currentUrl.replace(/\.lesson.*/, '.lesson'));

        var pageMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
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
      }
    });
  }
);
