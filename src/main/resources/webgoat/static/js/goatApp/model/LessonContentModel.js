define([
    'jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel'
], function (
    $,
    _,
    Backbone,
    HTMLContentModel
) {

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

            // Use a simplified and constrained pattern to avoid catastrophic backtracking.
            // Original patterns:
            //   document.URL.replace(/\.lesson.*/, '.lesson')
            //   /.*\.lesson\/(\d{1,4})$/
            // The new patterns are anchored and limited in scope.
            var currentUrl = String(document.URL || '');

            // Normalize lessonUrl: strip any trailing path or query after the first ".lesson"
            // Pattern explanation:
            //   ^(.*?\.lesson).*$
            //   - non-greedy group up to ".lesson" and discard the rest.
            var lessonUrlMatch = currentUrl.match(/^(.*?\.lesson).*$/);
            var lessonUrl = lessonUrlMatch ? lessonUrlMatch[1] : currentUrl;
            this.set('lessonUrl', lessonUrl);

            // Extract pageNum if URL looks like: "...something.lesson/<1-4 digit number>"
            // Pattern is simplified and does not use leading ".*":
            //   \.lesson\/(\d{1,4})$
            var pageNum = 0;
            var pageMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
            if (pageMatch) {
                pageNum = parseInt(pageMatch[1], 10) || 0;
            }
            this.set('pageNum', pageNum);

            this.trigger('content:loaded', this, loadHelps);
        },

        fetch: function (options) {
            options = options || {};
            return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: "html" }, options));
        }
    });
});
