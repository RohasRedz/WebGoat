define(
  ['jquery', 'underscore', 'backbone', 'goatApp/model/HTMLContentModel'],
  function ($, _, Backbone, HTMLContentModel) {
    'use strict';

    /**
     * Utility: get a safe URL representation for the current location.
     * Ensures we don't process unexpected multi-encoded values and
     * defensively handle malformed URLs.
     */
    function getSafeDocumentUrl() {
      try {
        // Use the full href; avoid relying on partially decoded values
        return window.location && window.location.href ? String(window.location.href) : '';
      } catch (e) {
        return '';
      }
    }

    return HTMLContentModel.extend({
      urlRoot: null,
      defaults: {
        items: null,
        selectedItem: null
      },

      initialize: function (options) {
        // No-op; preserving original behavior
      },

      loadData: function (options) {
        // Preserve original semantics: encode the lesson name safely
        // _.escape is for HTML, encodeURIComponent is for URLs; we only need URL encoding here.
        var safeName = encodeURIComponent(String(options.name || ''));
        this.urlRoot = safeName + '.lesson';

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

        var currentUrl = getSafeDocumentUrl();

        // Derive the lesson URL by replacing the `.lesson...` suffix with `.lesson`
        // Use a constrained, efficient regex to avoid catastrophic backtracking.
        // Original: document.URL.replace(/\.lesson.*/,'.lesson')
        var lessonUrl = currentUrl.replace(/\.lesson(?:\/.*)?$/, '.lesson');
        this.set('lessonUrl', lessonUrl);

        // Extract an optional page number from a URL that ends with `.lesson/<1-4 digits>`
        // Original pattern: /.*\.lesson\/(\d{1,4})$/
        // We use a safer, anchored regexp with minimal backtracking.
        var pageNum = 0;
        var pageMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
        if (pageMatch && pageMatch[1]) {
          pageNum = parseInt(pageMatch[1], 10) || 0;
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
      }
    });
  }
);
