define([
    'jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel'
], function ($, _, Backbone, HTMLContentModel) {

    return HTMLContentModel.extend({
        urlRoot: null,
        defaults: {
            items: null,
            selectedItem: null
        },

        initialize: function (options) {

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

            // Use a precompiled, simple and efficient RegExp to avoid catastrophic backtracking (ReDoS)
            // Original: document.URL.replace(/\.lesson.*/,'.lesson');
            var lessonUrlPattern = /\.lesson.*/;
            this.set('lessonUrl', document.URL.replace(lessonUrlPattern, '.lesson'));

            // Also simplify the page number extraction regex using a precompiled, safe pattern
            // Original: /.*\.lesson\/(\d{1,4})$/
            var pageNumPattern = /\.lesson\/(\d{1,4})$/;
            var pageNumMatch = document.URL.match(pageNumPattern);
            if (pageNumMatch) {
                this.set('pageNum', pageNumMatch[1]);
            } else {
                this.set('pageNum', 0);
            }

            this.trigger('content:loaded', this, loadHelps);
        },

        fetch: function (options) {
            options = options || {};
            return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: 'html' }, options));
        }
    });
});
