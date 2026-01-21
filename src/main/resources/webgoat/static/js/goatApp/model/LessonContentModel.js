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

            // Use a safer, linear-time pattern and pre-parse the URL instead of
            // repeatedly applying complex regex directly to document.URL
            var currentUrl;
            try {
                currentUrl = window.location.href;
            } catch (e) {
                currentUrl = document.URL;
            }

            // Derive lessonUrl safely without relying on a backtracking‑prone pattern
            var lessonUrl = currentUrl.split('.lesson')[0] + '.lesson';
            this.set('lessonUrl', lessonUrl);

            // Extract pageNum via a simpler, non‑catastrophic pattern
            var pageNum = 0;
            var pageMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
            if (pageMatch && pageMatch[1]) {
                pageNum = parseInt(pageMatch[1], 10);
                if (!Number.isFinite(pageNum) || pageNum < 0) {
                    pageNum = 0;
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
