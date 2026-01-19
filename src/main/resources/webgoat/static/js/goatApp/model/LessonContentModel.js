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

            // Hardened URL handling to avoid inefficient regular expressions
            var currentUrl = document.URL || '';
            var lessonUrl = currentUrl;
            var lessonIndex = currentUrl.indexOf('.lesson');
            if (lessonIndex !== -1) {
                lessonUrl = currentUrl.substring(0, lessonIndex + '.lesson'.length);
            }
            this.set('lessonUrl', lessonUrl);

            var pageNum = 0;
            var lastSlash = currentUrl.lastIndexOf('/');
            if (lastSlash !== -1) {
                var maybePage = currentUrl.substring(lastSlash + 1);
                // Accept only 1–4 digit numeric page segments
                var pageMatch = maybePage.match(/^[0-9]{1,4}$/);
                if (pageMatch) {
                    pageNum = parseInt(pageMatch[0], 10);
                }
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
