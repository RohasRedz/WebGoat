define(
  ['jquery', 'underscore', 'backbone', 'goatApp/model/HTMLContentModel'],
  function ($, _, Backbone, HTMLContentModel) {
    return HTMLContentModel.extend({
      urlRoot: null,
      defaults: {
        items: null,
        selectedItem: null
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

        // Use a simplified, more efficient regex to avoid catastrophic backtracking
        // Original:
        // this.set('lessonUrl', document.URL.replace(/\.lesson.*/, '.lesson'));
        // if (/.*\.lesson\/(\d{1,4})$/.test(document.URL)) {
        //   this.set('pageNum', document.URL.replace(/.*\.lesson\/(\d{1,4})$/, '$1'));
        // } else {
        //   this.set('pageNum', 0);
        // }

        var url = document.URL;

        // Normalize by splitting at ".lesson" instead of using a greedy regex
        var lessonIndex = url.indexOf('.lesson');
        if (lessonIndex !== -1) {
          this.set('lessonUrl', url.substring(0, lessonIndex + '.lesson'.length));
        } else {
          this.set('lessonUrl', url);
        }

        // Use a targeted, non-greedy pattern anchored to the end for page extraction
        var pageMatch = url.match(/\.lesson\/(\d{1,4})$/);
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
