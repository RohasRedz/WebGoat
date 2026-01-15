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
            // Ensure lesson name is treated as plain text and not as a path/URL fragment
            var safeName = String(options.name || '');
            // Basic allowlist: only allow simple word characters, dashes, underscores and dots
            safeName = safeName.replace(/[^\w.-]/g, '');

            // Use encodeURIComponent first, then escape, and avoid composing paths directly from raw input
            this.urlRoot = _.escape(encodeURIComponent(safeName)) + '.lesson';

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

            var currentUrl = String(document.URL || '');

            // Derive lessonUrl in a safer, more explicit way without over-broad regex
            var lessonUrl = currentUrl.replace(/\.lesson(?:\/.*)?$/, '.lesson');
            this.set('lessonUrl', lessonUrl);

            // More constrained page number extraction; avoid catastrophic backtracking
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
                _.extend({ dataType: 'html' }, options)
            );
        }
    });
});
