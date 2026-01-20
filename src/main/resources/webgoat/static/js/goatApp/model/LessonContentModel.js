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
            // Use encodeURIComponent directly and avoid unnecessary double-escaping
            var safeName = encodeURIComponent(options.name);
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

            // Use a more efficient, non-catastrophic regular expression for lessonUrl and pageNum
            var currentUrl = document.URL;

            // Normalize URL once to avoid repeated work
            // Replace the trailing ".lesson" (with or without a following "/<digits>") safely
            var lessonUrl = currentUrl.replace(/\.lesson(?:\/\d+)?$/, '.lesson');
            this.set('lessonUrl', lessonUrl);

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
