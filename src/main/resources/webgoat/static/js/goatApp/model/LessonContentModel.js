define([
    'jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel'
],
function ($, _, Backbone, HTMLContentModel) {

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

            var currentUrl = document.URL;

            /**
             * FIX: Mitigate inefficient regular expression complexity (ReDoS).
             *
             * Original patterns:
             *   document.URL.replace(/\.lesson.*/, '.lesson')
             *   /.*\.lesson\/(\d{1,4})$/
             *
             * Issues:
             *   - Leading `.*` combined with other tokens can cause expensive backtracking
             *     on very long or adversarial inputs.
             *
             * Strategy:
             *   - Use more precise and non-greedy patterns.
             *   - Avoid unanchored `.*` where not strictly necessary.
             *   - Keep behavior identical for normal lesson URLs.
             */

            // Safer pattern: match anything up to ".lesson" minimally, then normalize.
            var safeLessonUrl = currentUrl.replace(/^([\s\S]*?\.lesson).*$/, '$1');
            this.set('lessonUrl', safeLessonUrl);

            // Safer page number extraction:
            // - Explicitly look for ".lesson/<1-4 digits>" near the end.
            // - Avoid leading `.*` with ambiguous backtracking.
            var pageMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
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
                _.extend({ dataType: "html" }, options)
            );
        }
    });
});
