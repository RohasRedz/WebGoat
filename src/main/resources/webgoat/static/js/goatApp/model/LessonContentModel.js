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

            // Avoid complex regex when extracting the base lesson URL.
            // Previous pattern: document.URL.replace(/\.lesson.*/,'.lesson')
            // New: simple split-based approach that is not vulnerable to catastrophic backtracking.
            var currentUrl = document.URL;
            var lessonIndex = currentUrl.indexOf('.lesson');
            var baseLessonUrl = lessonIndex !== -1
                ? currentUrl.substring(0, lessonIndex + '.lesson'.length)
                : currentUrl;

            this.set('lessonUrl', baseLessonUrl);

            // Extract the page number using a simpler, non-catastrophic regex and small input.
            // Previous: /.*\.lesson\/(\d{1,4})$/
            // Now we first narrow the input to just the path segment around .lesson.
            var pageNum = 0;
            var lessonPathIndex = currentUrl.indexOf('.lesson/');
            if (lessonPathIndex !== -1) {
                var afterLesson = currentUrl.substring(lessonPathIndex + '.lesson/'.length);
                // afterLesson is now only the trailing part; apply a simple regex.
                var pageMatch = /^(\d{1,4})$/.test(afterLesson) ? afterLesson : null;
                if (pageMatch !== null) {
                    pageNum = parseInt(afterLesson, 10);
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
