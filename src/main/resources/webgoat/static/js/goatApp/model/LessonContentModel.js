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

            // Derive lesson URL in a safe and efficient way
            var currentUrl = document.URL;
            var lessonUrl;

            // Avoid using complex, potentially catastrophic backtracking regex.
            // Instead, check for the ".lesson" suffix and replace it directly.
            if (currentUrl.indexOf('.lesson') !== -1) {
                lessonUrl = currentUrl.substring(0, currentUrl.indexOf('.lesson')) + '.lesson';
            } else {
                lessonUrl = currentUrl;
            }
            this.set('lessonUrl', lessonUrl);

            // Extract page number using a simplified and bounded pattern
            var pageNum = 0;
            var pageMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
            if (pageMatch && pageMatch[1]) {
                pageNum = parseInt(pageMatch[1], 10);
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
