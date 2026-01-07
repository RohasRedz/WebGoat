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

            // Use an efficient, non-backtracking-prone regex and avoid re-compiling inline
            var url = document.URL;
            // Match ".lesson" followed by an optional "/<1-4 digit page number>"
            var lessonUrlMatch = url.match(/\.lesson(?:\/\d{1,4})?$/);
            if (lessonUrlMatch) {
                // Replace the entire matched suffix with ".lesson"
                this.set('lessonUrl', url.replace(lessonUrlMatch[0], '.lesson'));
            } else {
                // Fallback: simple suffix replacement without complex regex
                if (url.indexOf('.lesson') !== -1) {
                    this.set('lessonUrl', url.substring(0, url.indexOf('.lesson') + '.lesson'.length));
                } else {
                    this.set('lessonUrl', url);
                }
            }

            // Extract page number (1-4 digits at the end, after ".lesson/")
            var pageNumMatch = url.match(/\.lesson\/(\d{1,4})$/);
            if (pageNumMatch) {
                this.set('pageNum', pageNumMatch[1]);
            } else {
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
