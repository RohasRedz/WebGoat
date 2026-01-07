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

            // Hardened URL handling to avoid complex regular expression backtracking
            var currentUrl = String(document.URL || '');
            // Strip any trailing "/<digits>" segment if present, without heavy regex
            var baseLessonUrl = currentUrl;
            var lastSlashIdx = currentUrl.lastIndexOf('/');
            if (lastSlashIdx !== -1) {
                var lastSegment = currentUrl.substring(lastSlashIdx + 1);
                // Last segment looks like a page number (1-4 digits)
                if (/^\d{1,4}$/.test(lastSegment)) {
                    baseLessonUrl = currentUrl.substring(0, lastSlashIdx);
                }
            }

            // Ensure we produce "<something>.lesson" safely without complex regex
            if (baseLessonUrl.indexOf('.lesson') === -1) {
                // Fallback: if no ".lesson" present, use original URL as-is
                this.set('lessonUrl', currentUrl);
            } else {
                // Truncate everything after ".lesson"
                var lessonIndex = baseLessonUrl.indexOf('.lesson');
                this.set('lessonUrl', baseLessonUrl.substring(0, lessonIndex + '.lesson'.length));
            }

            // Determine page number using a simple check on the last path segment
            var pageNum = 0;
            var lastSlashIdx2 = currentUrl.lastIndexOf('/');
            if (lastSlashIdx2 !== -1) {
                var lastSegment2 = currentUrl.substring(lastSlashIdx2 + 1);
                if (/^\d{1,4}$/.test(lastSegment2)) {
                    pageNum = parseInt(lastSegment2, 10);
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
