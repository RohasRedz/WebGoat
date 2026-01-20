define([
    'jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel'
], function($, _, Backbone, HTMLContentModel) {

    return HTMLContentModel.extend({
        urlRoot: null,
        defaults: {
            items: null,
            selectedItem: null
        },

        initialize: function (options) {
        },

        loadData: function(options) {
            this.urlRoot = _.escape(encodeURIComponent(options.name)) + '.lesson';
            var self = this;
            this.fetch().done(function(data) {
                self.setContent(data);
            });
        },

        setContent: function(content, loadHelps) {
            if (typeof loadHelps === 'undefined') {
                loadHelps = true;
            }
            this.set('content', content);

            // Use a simpler, linear-time-safe pattern for replacing the .lesson suffix
            var currentUrl = document.URL;
            this.set(
                'lessonUrl',
                currentUrl.endsWith('.lesson')
                    ? currentUrl.slice(0, -('.lesson'.length)) + '.lesson'
                    : currentUrl
            );

            // Use non-backtracking-safe checks for page number extraction
            var pageMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
            if (pageMatch && pageMatch[1]) {
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
                _.extend({ dataType: "html" }, options)
            );
        }
    });
});
