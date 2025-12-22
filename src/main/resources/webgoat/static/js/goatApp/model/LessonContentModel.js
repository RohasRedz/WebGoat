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

            // Use a precompiled, bounded regex to mitigate potential ReDoS on arbitrary URLs
            // Pattern: any characters up to ".lesson", then replace the trailing part with ".lesson"
            var lessonUrlPattern = /^(.+?\.lesson)(?:\/.*)?$/;
            var currentUrl = String(document.URL);
            var lessonUrlMatch = lessonUrlPattern.exec(currentUrl);
            if (lessonUrlMatch) {
                this.set('lessonUrl', lessonUrlMatch[1]);
            } else {
                // Fallback: original replacement, applied only if a simple ".lesson" segment exists
                this.set('lessonUrl', currentUrl.replace(/\.lesson.*/,'.lesson'));
            }

            // Restrict page number extraction to a safer, precompiled regex with explicit anchors
            var pageNumPattern = /^(.*\.lesson\/)(\d{1,4})$/;
            var pageNumMatch = pageNumPattern.exec(currentUrl);
            if (pageNumMatch) {
                this.set('pageNum', pageNumMatch[2]);
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
