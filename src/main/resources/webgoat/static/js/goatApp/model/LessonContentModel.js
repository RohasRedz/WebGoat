define(['jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel'],
     function($,
        _,
        Backbone,
        HTMLContentModel){

    // Maximum URL length to process to avoid potential ReDoS on extremely long URLs
    var MAX_URL_LENGTH = 2048;

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

            var currentUrl = document.URL || '';
            if (currentUrl.length > MAX_URL_LENGTH) {
                // Truncate to safe length before applying regex operations
                currentUrl = currentUrl.substring(0, MAX_URL_LENGTH);
            }

            // Use precompiled, linear-time-safe regexes and avoid unnecessary backtracking
            var lessonUrl = currentUrl.replace(/\.lesson.*/, '.lesson');
            this.set('lessonUrl', lessonUrl);

            var pageNumMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
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
