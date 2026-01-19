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

            // Use URL API for safer parsing instead of complex regexes on document.URL
            var href = document.URL;
            var lessonUrl = href;
            var pageNum = 0;

            try {
                var urlObj = new URL(href, window.location.origin);

                // Remove any trailing `/number` segment only if it is purely numeric (1 to 4 digits)
                var path = urlObj.pathname;
                var pageSegmentMatch = path.match(/\/(\d{1,4})$/);
                if (pageSegmentMatch) {
                    // Extract page number from last path segment
                    var pageSegment = pageSegmentMatch[1];
                    pageNum = parseInt(pageSegment, 10);

                    // Strip the numeric segment from the path
                    path = path.slice(0, -pageSegmentMatch[0].length);
                }

                // Normalize `.lesson` suffix using simple suffix check instead of regex
                if (path.endsWith('.lesson')) {
                    lessonUrl = urlObj.origin + path;
                } else {
                    lessonUrl = urlObj.origin + path + '.lesson';
                }
            } catch (e) {
                // Fallback: if URL API is not available, keep original document.URL
                lessonUrl = href;
                pageNum = 0;
            }

            this.set('lessonUrl', lessonUrl);
            this.set('pageNum', isNaN(pageNum) ? 0 : pageNum);
            this.trigger('content:loaded',this,loadHelps);
        },

        fetch: function (options) {
            options = options || {};
            return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: "html"}, options));
        }
    });
});
