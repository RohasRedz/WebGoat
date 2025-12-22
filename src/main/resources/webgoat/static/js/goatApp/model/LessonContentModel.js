define([
    'jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel'
], function ($, _, Backbone, HTMLContentModel) {

    var LESSON_URL_REGEX = /\.lesson(\/\d{1,4})?$/;
    var PAGE_NUM_REGEX = /.*\.lesson\/(\d{1,4})$/;

    return HTMLContentModel.extend({
        urlRoot: null,
        defaults: {
            items: null,
            selectedItem: null
        },

        initialize: function (options) {

        },

        loadData: function (options) {
            var rawName = (options && typeof options.name === 'string') ? options.name : '';
            this.urlRoot = _.escape(encodeURIComponent(rawName)) + '.lesson';

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

            if (LESSON_URL_REGEX.test(currentUrl)) {
                this.set('lessonUrl', currentUrl.replace(LESSON_URL_REGEX, '.lesson'));
            } else {
                this.set('lessonUrl', currentUrl);
            }

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
            return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: 'html' }, options));
        }
    });
});
