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
    'use strict';

    /**
     * Normalize and validate lesson name input to avoid ReDoS and
     * reduce the risk of injection into URLs.
     *
     * - Enforces a safe character set: letters, digits, underscore, hyphen, and dot.
     * - Enforces a reasonable maximum length.
     * - Rejects inputs that do not conform to this pattern.
     */
    function sanitizeLessonName(rawName) {
        if (typeof rawName !== 'string') {
            return null;
        }

        // Trim whitespace and enforce length limits
        var name = $.trim(rawName);
        if (!name.length || name.length > 128) {
            return null;
        }

        // Allow only a restricted character set to avoid pathological regex cases
        // and to prevent unexpected characters in lesson URLs.
        var safeNamePattern = /^[A-Za-z0-9_.-]+$/;
        if (!safeNamePattern.test(name)) {
            return null;
        }

        return name;
    }

    return HTMLContentModel.extend({
        urlRoot: null,
        defaults: {
            items: null,
            selectedItem: null
        },

        initialize: function (options) {
            // Intentionally left blank; no unsafe side effects.
        },

        loadData: function (options) {
            options = options || {};
            var sanitizedName = sanitizeLessonName(options.name);

            if (!sanitizedName) {
                // Fail safely: do not attempt to fetch content with an invalid name.
                // Do not log the raw input; just emit a generic error.
                if (window && window.console) {
                    console.error('Invalid lesson name provided; cannot load data.');
                }
                return;
            }

            // Use a simple, non-nested encoding approach and avoid constructing
            // unnecessarily complex regular expressions.
            this.urlRoot = encodeURIComponent(sanitizedName) + '.lesson';

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

            // Use a simpler, bounded regex without ambiguous backtracking patterns.
            var currentUrl = String(document.URL || '');
            this.set('lessonUrl', currentUrl.replace(/\.lesson.*/, '.lesson'));

            var pageNumMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
            if (pageNumMatch && pageNumMatch[1]) {
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
                _.extend({ dataType: 'html' }, options)
            );
        }
    });
});
