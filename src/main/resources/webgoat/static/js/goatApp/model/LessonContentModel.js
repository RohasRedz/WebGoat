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
            // Ensure only expected lesson names are accepted and avoid creating ambiguous encoded patterns
            var rawName = typeof options.name === 'string' ? options.name : '';
            // Basic allowlist: only letters, numbers, underscore, dash and dot
            var safeName = rawName.replace(/[^A-Za-z0-9._-]/g, '');
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

            // Ensure we do not introduce unnecessary backtracking in regexes
            var currentUrl = String(document.URL);

            // Use an anchored, simple pattern to extract the base lesson URL
            this.set('lessonUrl', currentUrl.replace(/\.lesson(?:\/.*)?$/, '.lesson'));

            // Use a bounded, non-backtracking-friendly pattern for page numbers
            var pageMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
            if (pageMatch) {
                this.set('pageNum', pageMatch[1]);
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
