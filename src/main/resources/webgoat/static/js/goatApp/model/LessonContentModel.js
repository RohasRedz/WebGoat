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

            // Use safe, precompiled regular expressions and avoid complex patterns that
            // could lead to catastrophic backtracking (ReDoS), while preserving behavior.
            var pageUrl = document.URL;
            var lessonUrlPattern = /\.lesson.*/;
            var pageNumPattern = /.*\.lesson\/(\d{1,4})$/;

            this.set('lessonUrl', pageUrl.replace(lessonUrlPattern, '.lesson'));

            var pageNumMatch = pageNumPattern.exec(pageUrl);
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
