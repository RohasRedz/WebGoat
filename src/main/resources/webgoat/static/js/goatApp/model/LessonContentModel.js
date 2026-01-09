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
            // Ensure lesson name is treated as a safe path segment and not used as raw regex/HTML
            var safeName = String(options.name || '').trim();

            // Encode as URI component to avoid special characters affecting routing
            // Do NOT use as a regex pattern directly; we only build a simple path
            this.urlRoot = encodeURIComponent(safeName) + '.lesson';

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

            // Avoid complex, potentially catastrophic backtracking regexps.
            // Use simpler, more controlled parsing for lesson URL and page number.
            var currentUrl = document.URL;
            var baseLessonUrl = currentUrl;
            var pageNum = 0;

            // Extract base lesson URL without trailing /<page>
            // Example: https://host/app/lesson/Some.lesson/3 -> https://host/app/lesson/Some.lesson
            var lastSlashIndex = currentUrl.lastIndexOf('/');
            if (lastSlashIndex > -1) {
                var lastSegment = currentUrl.substring(lastSlashIndex + 1);
                var pageMatch = lastSegment.match(/^[0-9]{1,4}$/);
                if (pageMatch) {
                    // Strip the trailing numeric segment
                    baseLessonUrl = currentUrl.substring(0, lastSlashIndex);
                    pageNum = parseInt(pageMatch[0], 10) || 0;
                }
            }

            // Replace .lesson.* with .lesson to get canonical lesson URL
            baseLessonUrl = baseLessonUrl.replace(/\.lesson.*/, '.lesson');
            this.set('lessonUrl', baseLessonUrl);
            this.set('pageNum', pageNum);

            this.trigger('content:loaded',this,loadHelps);
        },

        fetch: function (options) {
            options = options || {};
            return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: "html"}, options));
        }
    });
});
