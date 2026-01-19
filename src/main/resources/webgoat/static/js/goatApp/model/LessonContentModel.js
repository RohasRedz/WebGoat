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

            // Use a safe constant lesson URL instead of a complex regex on document.URL
            this.set('lessonUrl', window.location.origin + window.location.pathname.replace(/\.lesson.*/, '.lesson'));

            // Derive pageNum using a simplified, bounded pattern and input validation
            var pageNum = 0;
            var match = window.location.pathname.match(/\/(\d{1,4})$/);
            if (match && match[1]) {
                var parsed = parseInt(match[1], 10);
                if (!isNaN(parsed) && parsed >= 0 && parsed <= 9999) {
                    pageNum = parsed;
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
