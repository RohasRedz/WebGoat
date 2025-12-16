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
            // No-op initializer kept for compatibility
        },

        loadData: function (options) {
            // Ensure the lesson name is safely encoded and free from characters
            // that could cause unsafe URL patterns.
            var rawName = options && options.name ? String(options.name) : '';
            // Basic length limit to avoid pathological inputs.
            if (rawName.length > 256) {
                rawName = rawName.slice(0, 256);
            }

            // Use encodeURIComponent once, then escape for safe use in the URL.
            var safeName = _.escape(encodeURIComponent(rawName));
            this.urlRoot = safeName + '.lesson';

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

            // Limit URL length to mitigate potential ReDoS on very long inputs.
            var currentUrl = String(document.URL);
            if (currentUrl.length > 2048) {
                currentUrl = currentUrl.slice(0, 2048);
            }

            // Use a simpler, linear-time regex pattern rather than compound patterns
            // that could lead to catastrophic backtracking on crafted input.
            //
            // Pattern: capture a numeric page number if present after ".lesson/"
            // Example: https://.../xyz.lesson/12 -> matches "12"
            var pageNumMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);

            this.set('lessonUrl', currentUrl.replace(/\.lesson.*/, '.lesson'));

            if (pageNumMatch) {
                this.set('pageNum', pageNumMatch[1]);
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
