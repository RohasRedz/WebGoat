define(
  ['jquery', 'underscore', 'backbone', 'goatApp/model/HTMLContentModel'],
  function ($, _, Backbone, HTMLContentModel) {
    'use strict';

    return HTMLContentModel.extend({
      urlRoot: null,
      defaults: {
        items: null,
        selectedItem: null,
      },

      initialize: function (options) {
        // no-op
      },

      loadData: function (options) {
        // Validate and sanitize lesson name to a safe identifier (letters, digits, _, -, . only)
        var rawName = options && typeof options.name === 'string' ? options.name : '';
        var safeName = rawName.replace(/[^a-zA-Z0-9_.-]/g, '');

        if (!safeName) {
          // Fall back to a safe default to avoid constructing a path from unsafe data
          safeName = 'index';
        }

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

        var currentUrl = String(document.URL || '');
        this.set('lessonUrl', currentUrl.replace(/\.lesson.*/, '.lesson'));

        var pageMatch = currentUrl.match(/.*\.lesson\/(\d{1,4})$/);
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
  }
);
