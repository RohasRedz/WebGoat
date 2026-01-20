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

        // Use a safer, more efficient URL handling approach
        var url = document.URL || '';
        this.set(
          'lessonUrl',
          url.replace(/\.lesson(?:\/.*)?$/, '.lesson')
        );

        // Extract pageNum without complex backtracking-prone patterns
        var pageNum = 0;
        var lastSlashIndex = url.lastIndexOf('/');
        if (lastSlashIndex !== -1 && lastSlashIndex + 1 < url.length) {
          var tail = url.substring(lastSlashIndex + 1);
          // Ensure up to 4 digits and nothing else
          var pageMatch = /^(\d{1,4})$/.exec(tail);
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
  }
);
