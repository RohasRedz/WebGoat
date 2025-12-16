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

            var currentUrl = String(document.URL || '');
            var lessonUrl = currentUrl;
            var pageNum = 0;

            lessonUrl = lessonUrl.replace(/\/(\d{1,4})$/, '');

            if (/\.lesson(?:\/)?$/.test(lessonUrl)) {
                lessonUrl = lessonUrl.replace(/\.lesson(?:\/)?$/, '.lesson');
            } else {
                lessonUrl = lessonUrl + '.lesson';
            }

            this.set('lessonUrl', lessonUrl);

            var pageMatch = currentUrl.match(/\/(\d{1,4})$/);
            if (pageMatch && pageMatch[1]) {
                pageNum = parseInt(pageMatch[1], 10) || 0;
            }

            this.set('pageNum', pageNum);
            this.trigger('content:loaded',this,loadHelps);
        },

        fetch: function (options) {
            options = options || {};
            return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: "html"}, options));
        }
    });
});
