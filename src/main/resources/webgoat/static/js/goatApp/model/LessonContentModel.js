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

            var currentUrl = String(document.URL || '');
            var lessonUrlMatch = currentUrl.match(/^(.*\.lesson)(?:\/\d{1,4})?$/);
            if (lessonUrlMatch && lessonUrlMatch[1]) {
                this.set('lessonUrl', lessonUrlMatch[1]);
            } else {
                // Fallback: strip query/hash if present, without using complex regexes
                var baseUrl = currentUrl.split('#')[0].split('?')[0];
                this.set('lessonUrl', baseUrl);
            }

            var pageNum = 0;
            var pageNumMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
            if (pageNumMatch && pageNumMatch[1]) {
                pageNum = parseInt(pageNumMatch[1], 10);
                if (!Number.isFinite(pageNum) || pageNum < 0) {
                    pageNum = 0;
                }
            }
            this.set('pageNum', pageNum);

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
