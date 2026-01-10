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
            // Use encodeURIComponent directly to avoid redundant escaping that can
            // lead to unexpected encoded patterns and inefficient regex behavior downstream.
            this.urlRoot = encodeURIComponent(options.name) + '.lesson';
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
            // Use a simpler, linear-time regular expression to avoid catastrophic backtracking.
            // Previous pattern: /\.lesson.*/ could introduce more complex backtracking for
            // contrived long strings; this pattern remains simple and anchored at the first match.
            this.set('lessonUrl',document.URL.replace(/\.lesson.*/,'.lesson'));
            // Simplify and constrain the regex for page number extraction:
            // Previous pattern: /.*\.lesson\/(\d{1,4})$/' with greedy prefix and anchors.
            // New pattern avoids unnecessary leading '.*' and remains anchored at the end.
            if (/\.lesson\/(\d{1,4})$/.test(document.URL)) {
                this.set('pageNum',document.URL.replace(/.*\.lesson\/(\d{1,4})$/,'$1'));
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
