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
            // Ensure options.name is treated as a plain, bounded string before using it
            var rawName = (options && typeof options.name === 'string') ? options.name : '';
            var safeName = rawName.trim();

            // Optionally enforce a maximum length and a limited character set to mitigate ReDoS
            if (safeName.length > 100) {
                safeName = safeName.substring(0, 100);
            }
            // Allow only simple, URL‑safe characters; drop anything else
            safeName = safeName.replace(/[^a-zA-Z0-9._-]/g, '');

            this.urlRoot = encodeURIComponent(safeName) + '.lesson';

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

            var currentUrl = String(document.URL);

            // Use simpler, non‑catastrophic regular expressions and avoid unnecessary backtracking
            var lessonUrlMatch = currentUrl.match(/\.lesson/);
            if (lessonUrlMatch) {
                this.set('lessonUrl', currentUrl.substring(0, lessonUrlMatch.index) + '.lesson');
            } else {
                this.set('lessonUrl', currentUrl);
            }

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
