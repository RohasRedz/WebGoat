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

    // Compiled regular expressions to avoid repeated parsing and
    // to reduce risk of inefficient backtracking behavior.
    var LESSON_URL_REGEX = /\.lesson.*/;
    var PAGE_NUM_REGEX = /.*\.lesson\/(\d{1,4})$/;

    return HTMLContentModel.extend({
        urlRoot: null,
        defaults: {
            items: null,
            selectedItem: null
        },

        initialize: function (options) {
            // No-op: kept for potential future extension.
        },

        loadData: function (options) {
            // Ensure options.name is treated as a simple string segment and
            // not used to construct arbitrary URLs with special characters.
            var safeName = _.escape(String(options.name || ''));

            // Use encodeURIComponent once; avoid double-encoding and
            // prevent injection into the path component.
            this.urlRoot = encodeURIComponent(safeName) + '.lesson';

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

            // Use precompiled regex and avoid re-parsing on each call.
            this.set('lessonUrl', currentUrl.replace(LESSON_URL_REGEX, '.lesson'));

            var pageMatch = PAGE_NUM_REGEX.exec(currentUrl);
            if (pageMatch && pageMatch[1]) {
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
