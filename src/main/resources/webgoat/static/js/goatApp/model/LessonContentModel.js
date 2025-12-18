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

            // Hardened regex to avoid catastrophic backtracking while
            // preserving original matching intent.
            //
            // Original patterns:
            //   \/\.lesson.*\/                any .lesson followed by anything
            //   \/.*\.lesson\/(\d{1,4})$\/    full URL pattern with trailing page number
            //
            // Both contained nested greedy patterns that can cause ReDoS on
            // very long, adversarial strings. We now:
            //   - Use character classes and tighter quantifiers.
            //   - Anchor patterns appropriately.
            //   - Avoid overlapping ".*" with other variable-length parts.
            var url = document.URL;

            // Replace any `.lesson` suffix and everything after it with `.lesson`
            // Example: ".../foo.lesson/123?x=1" -> ".../foo.lesson"
            var lessonUrl = url.replace(/\.lesson(?:[\/?#].*)?$/i, '.lesson');
            this.set('lessonUrl', lessonUrl);

            // Extract a trailing numeric page segment of length 14 if present.
            // Example: ".../foo.lesson/123" -> pageNum = "123"
            var pageMatch = url.match(/\.lesson\/([0-9]{1,4})(?:[\/?#].*)?$/i);
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
});
