define(['jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel'],
     function($,
        _,
        Backbone,
        HTMLContentModel){

    // NOTE:
    // This file has been updated to replace a potentially inefficient regular
    // expression on document.URL with a simpler, linear-time pattern while
    // preserving behavior.

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

            // Original:
            // this.set('lessonUrl',document.URL.replace(/\.lesson.*/,'.lesson'));
            //
            // The pattern `/\.lesson.*/` can, in some engines, produce inefficient
            // backtracking if the ".*" spans very long strings with certain inputs.
            // Behaviorally we just need to:
            //   - Find the first occurrence of ".lesson"
            //   - Keep everything up to and including that suffix
            //
            // Using a simple substring-based implementation avoids regex overhead
            // and guarantees linear behavior.
            (function() {
                var url = document.URL;
                var marker = '.lesson';
                var idx = url.indexOf(marker);
                if (idx !== -1) {
                    // Preserve everything up to and including ".lesson"
                    url = url.substring(0, idx + marker.length);
                }
                // If ".lesson" is not present, fall back to original URL
                // so that behavior remains backward compatible.
                // No regular expressions are needed here.
                this.set('lessonUrl', url);
            }).call(this);

            // Original:
            // if (/.*\.lesson\/(\d{1,4})$/.test(document.URL)) {
            //     this.set('pageNum',document.URL.replace(/.*\.lesson\/(\d{1,4})$/,'$1'));
            // } else {
            //     this.set('pageNum',0);
            // }
            //
            // The original pattern used ".*" with a capturing group, which,
            // while usually safe here, can be inefficient on very long strings.
            // We refactor it to a simpler, anchored pattern without leading ".*"
            // and use a single match() call instead of test() + replace().
            (function() {
                var url = document.URL;
                // Require ".lesson/" before the digits and capture the last 1–4 digits.
                // This avoids leading ".*" and catastrophic backtracking scenarios.
                var match = url.match(/\.lesson\/(\d{1,4})$/);
                if (match) {
                    this.set('pageNum', match[1]);
                } else {
                    this.set('pageNum', 0);
                }
            }).call(this);

            this.trigger('content:loaded',this,loadHelps);
        },

        fetch: function (options) {
            options = options || {};
            return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: "html"}, options));
        }
    });
});
