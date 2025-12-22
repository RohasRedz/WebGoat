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
            // Use simple, bounded parsing instead of complex regex to avoid ReDoS
            var lessonIndex = currentUrl.indexOf('.lesson');
            if (lessonIndex !== -1) {
                this.set('lessonUrl', currentUrl.substring(0, lessonIndex) + '.lesson');
            } else {
                this.set('lessonUrl', currentUrl);
            }

            // Extract pageNum using a safer, constrained pattern
            // Expect URLs like: <anything>.lesson/<1–4 digit number>
            var pageNum = 0;
            var lastSlash = currentUrl.lastIndexOf('/');
            if (lastSlash !== -1 && lastSlash + 1 < currentUrl.length) {
                var pageCandidate = currentUrl.substring(lastSlash + 1);
                // Only accept 1–4 digits
                if (/^\d{1,4}$/.test(pageCandidate)) {
                    pageNum = parseInt(pageCandidate, 10);
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
