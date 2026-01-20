define(['jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel'],
     function($,
        _,
        Backbone,
        HTMLContentModel){

    return HTMLContentModel.extend({
        urlRoot:null,
        defaults: {
            items:null,
            selectedItem:null
        },

        initialize: function (options) {

        },

        loadData: function(options) {
            // Ensure options.name is treated as a simple, bounded string before use
            var name = typeof options.name === 'string' ? options.name : '';
            name = name.trim();

            // Enforce a reasonable maximum length to avoid pathological cases
            if (name.length > 128) {
                name = name.substring(0, 128);
            }

            // Replace any path separator characters to prevent unintended path manipulation
            name = name.replace(/[\\/]/g, '_');

            // Use encodeURIComponent first, then escape, to keep the output safe and bounded
            this.urlRoot = _.escape(encodeURIComponent(name)) + '.lesson';

            var self = this;
            this.fetch().done(function(data) {
                self.setContent(data);
            });
        },

        setContent: function(content, loadHelps) {
            if (typeof loadHelps === 'undefined') {
                loadHelps = true;
            }
            this.set('content',content);

            // Use a constrained, non-backtracking-prone pattern for URL handling
            var currentUrl = String(document.URL);
            var lessonUrlMatch = currentUrl.match(/^(.*?\\.lesson)(?:\\/\\d{1,4})?$/);
            if (lessonUrlMatch) {
                this.set('lessonUrl', lessonUrlMatch[1]);
            } else {
                this.set('lessonUrl', currentUrl);
            }

            var pageMatch = currentUrl.match(/\\.lesson\\/(\\d{1,4})$/);
            if (pageMatch) {
                this.set('pageNum', pageMatch[1]);
            } else {
                this.set('pageNum', 0);
            }
            this.trigger('content:loaded',this,loadHelps);
        },

        fetch: function (options) {
            options = options || {};
            return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: "html"}, options));
        }
    });
});
