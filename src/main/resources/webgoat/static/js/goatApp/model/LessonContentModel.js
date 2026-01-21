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
            this.urlRoot = _.escape(encodeURIComponent(options.name)) + '.lesson'
            var self = this;
            this.fetch().done(function(data) {
                self.setContent(data);
            });
        },

        /**
         * Safely derive the base lesson URL from the current document location
         * without using potentially expensive or ambiguous regular expressions.
         */
        _computeLessonUrl: function () {
            var url = String(document.URL || '');
            var lessonIndex = url.indexOf('.lesson');
            if (lessonIndex === -1) {
                // No .lesson segment; return the original URL as a safe fallback.
                return url;
            }
            return url.substring(0, lessonIndex) + '.lesson';
        },

        /**
         * Safely extract the page number from the current document location.
         * Expects URLs of the form: <base>.lesson/<pageNum>
         * Falls back to 0 if the pattern does not match or is invalid.
         */
        _computePageNum: function () {
            var url = String(document.URL || '');
            var lessonIndex = url.indexOf('.lesson/');
            if (lessonIndex === -1) {
                return 0;
            }

            var pagePart = url.substring(lessonIndex + '.lesson/'.length);
            // Page number should be 14 digits only.
            if (/^\d{1,4}$/.test(pagePart)) {
                return parseInt(pagePart, 10);
            }

            return 0;
        },

        setContent: function(content, loadHelps) {
            if (typeof loadHelps === 'undefined') {
                loadHelps = true;
            }
            this.set('content',content);

            // Replaced regex-based URL manipulation with explicit string handling
            // to avoid inefficient regular expressions and potential ReDoS vectors.
            this.set('lessonUrl', this._computeLessonUrl());
            this.set('pageNum', this._computePageNum());

            this.trigger('content:loaded',this,loadHelps);
        },

        fetch: function (options) {
            options = options || {};
            return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: "html"}, options));
        }
    });
});
