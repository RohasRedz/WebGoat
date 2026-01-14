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

            // Use a simpler, non-backtracking-prone approach for extracting lesson URL and page number
            var currentUrl = document.URL;
            var baseLessonUrl = currentUrl;
            var pageNum = 0;

            // If the URL ends with /<digits>, treat that as the page number
            var lastSlashIndex = currentUrl.lastIndexOf('/');
            if (lastSlashIndex !== -1 && lastSlashIndex < currentUrl.length - 1) {
                var suffix = currentUrl.substring(lastSlashIndex + 1);
                // Ensure the suffix is reasonably short and numeric to avoid ReDoS-style abuse
                if (/^\d{1,4}$/.test(suffix)) {
                    pageNum = parseInt(suffix, 10);
                    baseLessonUrl = currentUrl.substring(0, lastSlashIndex);
                }
            }

            // Normalize the lesson URL to end with .lesson
            if (/\.lesson$/.test(baseLessonUrl)) {
                this.set('lessonUrl', baseLessonUrl);
            } else {
                this.set('lessonUrl', baseLessonUrl.replace(/\.lesson.*/, '.lesson'));
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
