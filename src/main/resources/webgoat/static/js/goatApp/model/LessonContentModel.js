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

            // Use simpler and safer URL parsing to avoid complex regex backtracking
            var currentUrl = document.URL;
            var lessonUrl = currentUrl.split('.lesson')[0] + '.lesson';
            this.set('lessonUrl', lessonUrl);

            // Extract page number using index-based parsing to avoid inefficient regex
            var pageNum = 0;
            var lessonSegmentIndex = currentUrl.indexOf('.lesson/');
            if (lessonSegmentIndex !== -1) {
                var pagePart = currentUrl.substring(lessonSegmentIndex + '.lesson/'.length);
                // Only accept 1–4 digit numeric page numbers
                if (/^[0-9]{1,4}$/.test(pagePart)) {
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
