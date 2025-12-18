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

            var currentUrl = document.URL || '';

            var lessonIndex = currentUrl.indexOf('.lesson');
            if (lessonIndex !== -1) {
                this.set('lessonUrl', currentUrl.substring(0, lessonIndex + '.lesson'.length));
            } else {
                this.set('lessonUrl', currentUrl);
            }

            var pageNum = 0;
            var lastSlash = currentUrl.lastIndexOf('/');
            if (lastSlash !== -1 && lastSlash < currentUrl.length - 1) {
                var tail = currentUrl.substring(lastSlash + 1);
                if (/^[0-9]{1,4}$/.test(tail)) {
                    pageNum = parseInt(tail, 10);
                }
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
