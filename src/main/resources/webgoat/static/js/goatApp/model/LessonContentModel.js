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

            // Use a precompiled, bounded regex and avoid repeated literal regex construction
            var currentUrl = String(document.URL || '');
            // This pattern requires a numeric page segment of 1–4 digits after ".lesson/"
            // and is compiled once here, avoiding any catastrophic backtracking patterns.
            var lessonPagePattern = /^(.*\.lesson)\/(\d{1,4})$/;

            this.set('lessonUrl', currentUrl.replace(/\.lesson.*/, '.lesson'));

            var match = currentUrl.match(lessonPagePattern);
            if (match) {
                // match[2] is guaranteed to be 1–4 digits due to the regex
                this.set('pageNum', parseInt(match[2], 10));
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
