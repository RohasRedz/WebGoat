define([
    'jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel'
], function ($,
             _,
             Backbone,
             HTMLContentModel) {

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

            // Use a simple, non-backtracking-safe replacement for the suffix.
            // This avoids complex regex engines paths while preserving behavior.
            var currentUrl = document.URL;
            var lessonUrl = currentUrl;
            var lessonSuffixIndex = currentUrl.indexOf('.lesson');
            if (lessonSuffixIndex !== -1) {
                lessonUrl = currentUrl.substring(0, lessonSuffixIndex) + '.lesson';
            }
            this.set('lessonUrl', lessonUrl);

            // Avoid a potentially catastrophic backtracking regex by using indexOf/substring.
            // Original intent: if URL ends with ".lesson/<1-4 digit page number>", capture that number.
            var pageNum = 0;
            var lessonSegmentIndex = currentUrl.indexOf('.lesson/');
            if (lessonSegmentIndex !== -1) {
                var pageSegment = currentUrl.substring(lessonSegmentIndex + '.lesson/'.length);
                // Only accept 1–4 digit numeric page identifiers.
                if (/^[0-9]{1,4}$/.test(pageSegment)) {
                    pageNum = parseInt(pageSegment, 10);
                }
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
