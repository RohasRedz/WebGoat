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

            // Use a simpler, non-backtracking pattern and avoid global stateful regex.
            // Matches ".lesson" followed by an optional "/" and digits at the end.
            var lessonPageRegex = /\.lesson(?:\/(\d{1,4}))?$/;

            var currentUrl = document.URL;
            var lessonUrl = currentUrl.replace(lessonPageRegex, '.lesson');
            this.set('lessonUrl', lessonUrl);

            var match = currentUrl.match(lessonPageRegex);
            if (match && match[1]) {
                // Safe, bounded page number extraction (1–4 digits)
                this.set('pageNum', match[1]);
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
