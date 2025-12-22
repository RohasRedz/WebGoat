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
        // Keep name encoding, but ensure it is treated as a path segment, not raw HTML
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

        // Use a simpler, bounded regex to avoid catastrophic backtracking.
        // Original: document.URL.replace(/\.lesson.*/, '.lesson')
        // New: only replace the first occurrence of ".lesson" safely.
        this.set(
          'lessonUrl',
          document.URL.replace(/\.lesson[^/]*/, '.lesson')
        );

        // Original pattern: /.*\.lesson\/(\d{1,4})$/
        // This pattern is already bounded (\d{1,4}) and not prone to catastrophic backtracking.
        // Keep it, but avoid redundant leading '.*' by anchoring more simply.
        var pageNumMatch = document.URL.match(/\.lesson\/(\d{1,4})$/);
        if (pageNumMatch) {
          this.set('pageNum', pageNumMatch[1]);
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
  }
);
