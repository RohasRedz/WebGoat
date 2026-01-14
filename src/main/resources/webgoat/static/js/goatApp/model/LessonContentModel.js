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

            var currentUrl = String(document.URL || '');
            // Use simpler, bounded operations to avoid complex backtracking patterns
            var lessonUrl = currentUrl;
            var lessonSuffixIndex = currentUrl.indexOf('.lesson');
            if (lessonSuffixIndex !== -1) {
                lessonUrl = currentUrl.substring(0, lessonSuffixIndex + '.lesson'.length);
            }
            this.set('lessonUrl', lessonUrl);

            // Extract page number using safer parsing instead of a potentially expensive regex
            var pageNum = 0;
            var lessonPathIndex = currentUrl.indexOf('.lesson/');
            if (lessonPathIndex !== -1) {
                var pagePart = currentUrl.substring(lessonPathIndex + '.lesson/'.length);
                // Expect only digits after ".lesson/"
                if (/^\d{1,4}$/.test(pagePart)) {
                    pageNum = parseInt(pagePart, 10);
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
