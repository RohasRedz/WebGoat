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

            // Use a safer, more explicit URL parsing approach to avoid
            // regex-based performance issues and to better constrain input.
            (function(model) {
                try {
                    var currentUrl = window.location.href || String(document.URL || '');
                    var urlObj;
                    try {
                        urlObj = new URL(currentUrl);
                    } catch (e) {
                        // Fallback for environments without URL support
                        urlObj = { href: currentUrl, pathname: currentUrl, origin: '' };
                    }

                    var href = urlObj.href;
                    var pathname = urlObj.pathname || '';
                    var origin = urlObj.origin || '';

                    // Derive base lesson URL in a controlled way:
                    // - Ensure we're only working with paths containing ".lesson"
                    // - Avoid overly broad regexes that can cause backtracking issues
                    var lessonIndex = pathname.indexOf('.lesson');
                    var basePath;
                    if (lessonIndex !== -1) {
                        basePath = pathname.substring(0, lessonIndex + '.lesson'.length);
                    } else {
                        // Fallback: use the original pathname if no .lesson segment is found
                        basePath = pathname;
                    }

                    var safeLessonUrl = origin + basePath;
                    model.set('lessonUrl', safeLessonUrl);

                    // Extract page number using a bounded, simple regex and explicit checks
                    var pageNum = 0;
                    var pageMatch = pathname.match(/\.lesson\/([0-9]{1,4})$/);
                    if (pageMatch && pageMatch[1]) {
                        pageNum = parseInt(pageMatch[1], 10);
                        if (!Number.isFinite(pageNum) || pageNum < 0) {
                            pageNum = 0;
                        }
                    }
                    model.set('pageNum', pageNum);
                } catch (e) {
                    // On any parsing error, fall back to safe defaults
                    model.set('lessonUrl', '');
                    model.set('pageNum', 0);
                }
            })(this);

            this.trigger('content:loaded',this,loadHelps);
        },

        fetch: function (options) {
            options = options || {};
            return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: "html"}, options));
        }
    });
});
