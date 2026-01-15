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
            // Use encodeURIComponent directly, avoid double encoding and handle basic normalization
            var safeName = encodeURIComponent(String(options.name || ''));

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

            // Defensive handling of document.URL without overly complex regex
            var currentUrl = String(document.URL || '');
            var baseLessonUrl = currentUrl.split('.lesson')[0] + '.lesson';
            this.set('lessonUrl', baseLessonUrl);

            var pageNumMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
            if (pageNumMatch) {
                this.set('pageNum', pageNumMatch[1]);
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
