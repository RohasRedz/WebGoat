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

            var currentUrl = document.URL;

            this.set('lessonUrl', currentUrl.replace(/\.lesson(?:\/.+)?$/, '.lesson'));

            var pagePattern = /.*\.lesson\/(\d{1,4})$/;
            var match = pagePattern.exec(currentUrl);

            if (match) {
                this.set('pageNum', match[1]);
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
