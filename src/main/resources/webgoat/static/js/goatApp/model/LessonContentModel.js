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

            // Precompute URL once to avoid running heavy regexes multiple times
            var currentUrl = document.URL;

            // Use a safer, bounded pattern for lesson URL replacement:
            // - Anchors the match to the end of the string
            // - Restricts the allowed suffix to known safe characters
            // This reduces risk of catastrophic backtracking compared to a broad `.*` pattern.
            this.set(
                'lessonUrl',
                currentUrl.replace(/\.lesson(?:\/[A-Za-z0-9._~-]+)?$/, '.lesson')
            );

            // Use a simple, bounded pattern to extract the page number at most 4 digits at the end
            // This avoids overly permissive `.*` patterns and keeps the regex linear-time.
            var pageMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
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
