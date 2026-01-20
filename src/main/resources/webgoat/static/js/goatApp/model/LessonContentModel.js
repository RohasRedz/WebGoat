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
            this.urlRoot = _.escape(encodeURIComponent(options.name)) + '.lesson';
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

            // Use a safer, non-backtracking approach for deriving lesson URL and page number
            var url = String(document.URL || '');
            var lessonUrl = url;
            var pageNum = 0;

            // Normalize and parse using the URL API where available
            try {
                var parsed = new URL(url, window.location.origin);
                lessonUrl = parsed.origin + parsed.pathname.replace(/\.lesson\/?\d*$/, '.lesson');

                // Extract a trailing numeric page segment if present, bounded to 1-4 digits
                var segments = parsed.pathname.split('/');
                var lastSegment = segments[segments.length - 1];
                if (/^\d{1,4}$/.test(lastSegment)) {
                    pageNum = parseInt(lastSegment, 10);
                }
            } catch (e) {
                // Fallback for environments without URL support – avoid complex regex patterns
                lessonUrl = url.replace(/\.lesson\/?\d*$/, '.lesson');
                var simpleMatch = url.match(/\.lesson\/(\d{1,4})$/);
                if (simpleMatch && simpleMatch[1]) {
                    pageNum = parseInt(simpleMatch[1], 10);
                }
            }

            this.set('lessonUrl', lessonUrl);
            this.set('pageNum', isFinite(pageNum) ? pageNum : 0);

            this.trigger('content:loaded',this,loadHelps);
        },

        fetch: function (options) {
            options = options || {};
            return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: "html"}, options));
        }
    });
});
