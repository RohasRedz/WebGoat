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

            var currentUrl = document.URL;

            // Ensure we work with a safe, normalized URL string
            if (typeof currentUrl !== 'string') {
                currentUrl = '';
            }

            // Extract base lesson URL in a way that avoids catastrophic backtracking
            // Original pattern: \/\.lesson.\/*  (simple but fine)
            // Keep this, as it is not vulnerable to catastrophic backtracking:
            this.set('lessonUrl', currentUrl.replace(/\.lesson.*/, '.lesson'));

            // Safer extraction of pageNum using simple index-based parsing instead of a complex regex
            // Original vulnerable line:
            // if (/.*\.lesson\/(\d{1,4})$/.test(document.URL)) {
            //     this.set('pageNum',document.URL.replace(/.*\.lesson\/(\d{1,4})$/,'$1'));
            // } else {
            //     this.set('pageNum',0);
            // }
            //
            // New implementation: avoid a potentially expensive regex on attacker-controlled URL
            var pageNum = 0;
            var lessonIndex = currentUrl.indexOf('.lesson/');
            if (lessonIndex !== -1) {
                var pagePart = currentUrl.substring(lessonIndex + '.lesson/'.length);
                // Allow only 1–4 digit numeric page numbers
                var match = /^(\d{1,4})$/.exec(pagePart);
                if (match) {
                    pageNum = parseInt(match[1], 10);
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
