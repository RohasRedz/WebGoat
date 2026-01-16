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

            // Use a simpler, non-backtracking-prone pattern to compute the lesson URL
            var currentUrl = document.URL;
            var lessonUrl = currentUrl;

            // Replace the optional `/pageNumber` suffix if present (no nested quantifiers)
            // e.g., /somePath.lesson/12  -> /somePath.lesson
            //       /somePath.lesson     -> /somePath.lesson
            var lastLessonIndex = currentUrl.indexOf('.lesson');
            if (lastLessonIndex !== -1) {
                lessonUrl = currentUrl.substring(0, lastLessonIndex + '.lesson'.length);
            }
            this.set('lessonUrl', lessonUrl);

            // Extract page number in a safe way without complex regex patterns
            var pageNum = 0;
            var lastSlashIndex = currentUrl.lastIndexOf('/');
            if (lastSlashIndex > lastLessonIndex) {
                var possiblePage = currentUrl.substring(lastSlashIndex + 1);
                if (/^\d{1,4}$/.test(possiblePage)) {
                    pageNum = parseInt(possiblePage, 10);
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
