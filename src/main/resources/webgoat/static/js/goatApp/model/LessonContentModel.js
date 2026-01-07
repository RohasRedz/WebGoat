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

            // Use simple, efficient, and safe regex / string logic to avoid complex backtracking
            var currentUrl = document.URL;

            // Derive base lesson URL without using a complex backtracking-prone regex
            // 1) Strip trailing "/<pageNum>" if present
            var pageNumMatch = currentUrl.match(/\/(\d{1,4})$/);
            var baseUrl = currentUrl;
            if (pageNumMatch) {
                baseUrl = currentUrl.slice(0, -pageNumMatch[0].length);
            }

            // 2) Ensure we end with ".lesson"
            var lessonSuffixIndex = baseUrl.indexOf('.lesson');
            if (lessonSuffixIndex !== -1) {
                baseUrl = baseUrl.substring(0, lessonSuffixIndex + '.lesson'.length);
            }

            this.set('lessonUrl', baseUrl);

            // Extract page number efficiently if present
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
