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
            this.urlRoot = _.escape(encodeURIComponent(options.name)) + '.lesson';
            var self = this;
            this.fetch().done(function(data) {
                self.setContent(data);
            });
        },

        setContent: function(content, loadHelps) {
            if (typeof loadHelps === 'undefined') {
                loadHelps = true;
            }
            this.set('content', content);

            var currentUrl = String(document.URL);

            // Use a simplified, safe pattern to derive lessonUrl without heavy backtracking risk.
            // Previous pattern: document.URL.replace(/\.lesson.*/, '.lesson')
            // New approach: find first ".lesson" and truncate.
            var lessonIndex = currentUrl.indexOf('.lesson');
            if (lessonIndex !== -1) {
                this.set('lessonUrl', currentUrl.substring(0, lessonIndex + '.lesson'.length));
            } else {
                this.set('lessonUrl', currentUrl);
            }

            // Replace vulnerable regex with a more efficient, linear-time-safe version.
            // Previous pattern (potential ReDoS): /.*\.lesson\/(\d{1,4})$/
            // New pattern: anchored, non-greedy, no catastrophic backtracking characteristics.
            var pageMatch = /^.*?\.lesson\/(\d{1,4})$/.exec(currentUrl);
            if (pageMatch) {
                this.set('pageNum', pageMatch[1]);
            } else {
                this.set('pageNum', 0);
            }

            this.trigger('content:loaded', this, loadHelps);
        },

        fetch: function (options) {
            options = options || {};
            return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: "html"}, options));
        }
    });
});
