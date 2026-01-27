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
            this.set('content', content);

            // Hardened URL handling to avoid inefficient regular expressions on unbounded strings
            var currentUrl = document.URL;
            var lessonUrl = currentUrl;
            var pageNum = 0;

            try {
                var urlObj = new URL(currentUrl, window.location.origin);
                var pathname = urlObj.pathname || '';

                // Normalize lessonUrl: keep everything up to and including ".lesson"
                var lessonIndex = pathname.indexOf('.lesson');
                if (lessonIndex !== -1) {
                    var basePath = pathname.substring(0, lessonIndex + '.lesson'.length);
                    lessonUrl = urlObj.origin + basePath;
                } else {
                    lessonUrl = urlObj.origin + pathname;
                }

                // Derive pageNum from last path segment if it is a 1–4 digit number
                var lastSlashIndex = pathname.lastIndexOf('/');
                if (lastSlashIndex !== -1) {
                    var lastSegment = pathname.substring(lastSlashIndex + 1);
                    if (/^\d{1,4}$/.test(lastSegment)) {
                        pageNum = parseInt(lastSegment, 10);
                    }
                }
            } catch (e) {
                // Fallback to legacy behavior if URL parsing fails
                lessonUrl = currentUrl.replace(/\.lesson.*/, '.lesson');
                if (/.*\.lesson\/(\d{1,4})$/.test(currentUrl)) {
                    pageNum = parseInt(
                        currentUrl.replace(/.*\.lesson\/(\d{1,4})$/, '$1'),
                        10
                    ) || 0;
                } else {
                    pageNum = 0;
                }
            }

            this.set('lessonUrl', lessonUrl);
            this.set('pageNum', pageNum);

            this.trigger('content:loaded', this, loadHelps);
        },

        fetch: function (options) {
            options = options || {};
            return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: "html"}, options));
        }
    });
});
