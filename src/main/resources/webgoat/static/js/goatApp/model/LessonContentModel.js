define([
    'jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel'
], function ($, _, Backbone, HTMLContentModel) {
    'use strict';

    // Precompile and reuse safer, bounded regular expressions to avoid catastrophic backtracking
    var LESSON_URL_RE = /\.lesson(?:$|[?#])/; // match '.lesson' until end, query, or hash
    var PAGE_NUM_RE = /\.lesson\/(\d{1,4})$/; // strictly bounded digits 1-4

    return HTMLContentModel.extend({
        urlRoot: null,
        defaults: {
            items: null,
            selectedItem: null
        },

        initialize: function (options) {

        },

        loadData: function (options) {
            // keep functional behavior but ensure safe encoding of the lesson name
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

            // Use precompiled, safer regex for lesson URL replacement
            this.set('lessonUrl', currentUrl.replace(LESSON_URL_RE, '.lesson'));

            if (PAGE_NUM_RE.test(currentUrl)) {
                this.set('pageNum', currentUrl.replace(PAGE_NUM_RE, '$1'));
            } else {
                this.set('pageNum', 0);
            }
            this.trigger('content:loaded', this, loadHelps);
        },

        fetch: function (options) {
            options = options || {};
            return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: 'html' }, options));
        }
    });
});
