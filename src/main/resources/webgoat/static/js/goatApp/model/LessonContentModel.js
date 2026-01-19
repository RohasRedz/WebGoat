define(['jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel'],
     function($,
        _,
        Backbone,
        HTMLContentModel){

    return HTMLContentModel.extend({
        urlRoot:null,
        defaults: {
            items:null,
            selectedItem:null
        },

        initialize: function (options) {

        },

        loadData: function(options) {
            this.urlRoot = _.escape(encodeURIComponent(options.name)) + '.lesson'
            var self = this;
            this.fetch().done(function(data) {
                self.setContent(data);
            });
        },

        setContent: function(content, loadHelps) {
            if (typeof loadHelps === 'undefined') {
                loadHelps = true;
            }
            this.set('content',content);

            var currentUrl = document.URL;
            var lessonUrl = currentUrl;
            var pageNum = 0;

            var lastSlashIndex = currentUrl.lastIndexOf('/');
            if (lastSlashIndex !== -1) {
                var possiblePage = currentUrl.substring(lastSlashIndex + 1);
                if (/^\d{1,4}$/.test(possiblePage)) {
                    lessonUrl = currentUrl.substring(0, lastSlashIndex);
                    pageNum = parseInt(possiblePage, 10);
                }
            }
            lessonUrl = lessonUrl.replace(/\.lesson.*/, '.lesson');

            this.set('lessonUrl', lessonUrl);
            this.set('pageNum', pageNum);

            this.trigger('content:loaded',this,loadHelps);
        },

        fetch: function (options) {
            options = options || {};
            return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: "html"}, options));
        }
    });
});
