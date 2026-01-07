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

            // Use a more efficient, non-backtracking-safe approach for parsing the URL,
            // avoiding complex or potentially catastrophic regular expressions.
            var url = document.URL || '';
            try {
                var currentUrl = new URL(url);
                var pathname = currentUrl.pathname || '';

                // Normalize lesson URL by trimming any trailing page segment
                // e.g., /path/to/lesson/1234 -> /path/to/lesson
                var lessonPath = pathname.replace(/\/\d{1,4}$/, '');
                this.set('lessonUrl', lessonPath + '.lesson');

                // Extract page number from the final numeric path segment if present
                var pageNum = 0;
                var match = pathname.match(/\/(\d{1,4})$/);
                if (match && match[1]) {
                    pageNum = parseInt(match[1], 10) || 0;
                }
                this.set('pageNum', pageNum);
            } catch (e) {
                // Fallback to safe defaults if URL parsing fails
                this.set('lessonUrl', '.lesson');
                this.set('pageNum', 0);
            }

            this.trigger('content:loaded',this,loadHelps);
        },

        fetch: function (options) {
            options = options || {};
            return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: "html"}, options));
        }
    });
});
