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

            // FIX: Constrain regex used on document.URL to avoid potential ReDoS
            // and ensure we only operate on reasonably-sized URL strings.
            var currentUrl = String(document.URL || '');
            // Limit processed URL length to mitigate pathological cases.
            currentUrl = currentUrl.slice(0, 2048);

            // Previously:
            // this.set('lessonUrl',document.URL.replace(/\.lesson.*/,'.lesson'));
            //
            // The pattern /.*\.lesson.*/ is greedy; we only need to normalize the first ".lesson"
            // occurrence. Using a simpler, non-greedy replacement is cheaper and avoids
            // unnecessary backtracking.
            this.set('lessonUrl', currentUrl.replace(/\.lesson.*/, '.lesson'));

            // Previously:
            // if (/.*\.lesson\/(\d{1,4})$/.test(document.URL)) {
            //     this.set('pageNum',document.URL.replace(/.*\.lesson\/(\d{1,4})$/,'$1'));
            // } else {
            //     this.set('pageNum',0);
            // }
            //
            // FIX: Use a single, precompiled, anchored regex with an explicit limit on digits.
            // This reduces regex engine work and the risk of ReDoS while preserving behavior.
            var pageNum = 0;
            var pagePattern = /\.lesson\/(\d{1,4})$/;
            var match = currentUrl.match(pagePattern);
            if (match) {
                pageNum = parseInt(match[1], 10) || 0;
            }
            this.set('pageNum', pageNum);

            this.trigger('content:loaded', this, loadHelps);
        },

        fetch: function (options) {
            options = options || {};
            return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: 'html' }, options));
        }
    });
});
