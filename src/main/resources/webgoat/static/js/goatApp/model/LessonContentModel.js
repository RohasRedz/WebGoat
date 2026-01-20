define([
    'jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel'
], function ($, _, Backbone, HTMLContentModel) {

    /**
     * Safely derives the lesson URL from the current document URL
     * while avoiding potentially inefficient or catastrophic
     * regular expressions.
     *
     * @param {string} currentUrl
     * @returns {{lessonUrl: string, pageNum: number}}
     */
    function deriveLessonUrlAndPage(currentUrl) {
        if (typeof currentUrl !== 'string') {
            return {
                lessonUrl: '',
                pageNum: 0
            };
        }

        // Normalize URL once for consistent processing
        var url = currentUrl;

        // Derive lesson URL without using a complex regex
        // Replace a trailing ".lesson/<digits>" or ".lesson" with ".lesson"
        // using safe string operations.
        var lessonUrl = url;
        var lessonSuffixIndex = url.indexOf('.lesson');
        if (lessonSuffixIndex !== -1) {
            // Always end the lesson URL at the first ".lesson"
            lessonUrl = url.substring(0, lessonSuffixIndex + '.lesson'.length);
        }
        // Fallback: ensure it still ends with ".lesson" if that pattern exists elsewhere
        if (lessonUrl.indexOf('.lesson') === -1 && url.endsWith('.lesson')) {
            lessonUrl = url;
        }

        // Extract page number if the URL ends with ".lesson/<1-4 digit page>"
        var pageNum = 0;
        var lessonPageSeparator = '.lesson/';
        var separatorIndex = url.lastIndexOf(lessonPageSeparator);
        if (separatorIndex !== -1) {
            var pagePart = url.substring(separatorIndex + lessonPageSeparator.length);
            // Accept only 1–4 digits as page number
            if (/^\d{1,4}$/.test(pagePart)) {
                pageNum = parseInt(pagePart, 10);
            }
        }

        return {
            lessonUrl: lessonUrl,
            pageNum: pageNum
        };
    }

    return HTMLContentModel.extend({
        urlRoot: null,
        defaults: {
            items: null,
            selectedItem: null
        },

        initialize: function (options) {
            // No-op initializer; kept for API compatibility
        },

        loadData: function (options) {
            // Safely construct the URL root; ensure name is treated as plain text
            // and encoded for use as a path segment.
            var name = options && typeof options.name === 'string' ? options.name : '';
            this.urlRoot = encodeURIComponent(name) + '.lesson';
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

            var derived = deriveLessonUrlAndPage(document.URL);
            this.set('lessonUrl', derived.lessonUrl);
            this.set('pageNum', derived.pageNum);

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
});
