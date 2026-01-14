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

            // Use simpler, non-backtracking-prone patterns for URL normalization
            var url = document.URL;
            // Normalize lesson URL by stripping any suffix after ".lesson"
            var lessonIndex = url.indexOf('.lesson');
            if (lessonIndex !== -1) {
                this.set('lessonUrl', url.substring(0, lessonIndex + '.lesson'.length));
            } else {
                this.set('lessonUrl', url);
            }

            // Extract page number using a bounded, safe regex
            var pageMatch = url.match(/\.lesson\/([0-9]{1,4})$/);
            if (pageMatch) {
                this.set('pageNum', pageMatch[1]);
            } else {
                this.set('pageNum',0);
            }
            this.trigger('content:loaded',this,loadHelps);
        },

        fetch: function (options) {
            options = options || {};
            return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: "html"}, options));
        }
    });
});
