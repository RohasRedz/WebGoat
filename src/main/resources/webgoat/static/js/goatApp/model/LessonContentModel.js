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

        /**
         * Extract the lesson base URL and page number from the current location
         * in a more robust and efficient way than the previous broad regexes.
         */
        _parseLessonUrl: function () {
            var href = window.location && window.location.href ? window.location.href : (document.URL || '');
            // Normalize to avoid query/hash noise when computing the 'lessonUrl'
            // and page number. We work on the full href, but with explicit, simple
            // regex anchors to prevent inefficient backtracking.
            var baseLessonUrl;
            var pageNum = 0;

            // 1. Compute base lesson URL:
            //    Replace a trailing ".lesson" or ".lesson/<digits>" with ".lesson".
            //    Previous pattern: document.URL.replace(/\.lesson.*/,'.lesson')
            //    New patterns are anchored to the end or to a clear, bounded suffix.
            var lessonBaseRegex = /(\.lesson)(?:\/\d{1,4})?(?:[#?].*)?$/;
            if (lessonBaseRegex.test(href)) {
                baseLessonUrl = href.replace(lessonBaseRegex, '.lesson');
            } else {
                // Fallback: if it doesn't match expected patterns, use original href
                baseLessonUrl = href;
            }

            // 2. Extract page number (if present) from a trailing ".lesson/<1-4 digits>"
            //    Previous pattern: /.*\.lesson\/(\d{1,4})$/
            //    New pattern is simpler and anchored:
            var pageRegex = /\.lesson\/(\d{1,4})$/;
            var pageMatch = href.match(pageRegex);
            if (pageMatch && pageMatch[1]) {
                pageNum = parseInt(pageMatch[1], 10);
                if (!Number.isFinite(pageNum)) {
                    pageNum = 0;
                }
            }

            return {
                lessonUrl: baseLessonUrl,
                pageNum: pageNum
            };
        },

        setContent: function (content, loadHelps) {
            if (typeof loadHelps === 'undefined') {
                loadHelps = true;
            }
            this.set('content', content);

            var parsed = this._parseLessonUrl();
            this.set('lessonUrl', parsed.lessonUrl);
            this.set('pageNum', parsed.pageNum);

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
