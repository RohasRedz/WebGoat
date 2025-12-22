define(['jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel'],
     function($,
        _,
        Backbone,
        HTMLContentModel){

    // Precompile safe, bounded regular expressions to avoid repeated creation
    // and ensure they do not allow catastrophic backtracking.
    var LESSON_URL_REPLACE_REGEX = /\.lesson.*/;
    var LESSON_PAGE_NUM_REGEX = /\.lesson\/(\d{1,4})$/;

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

            // Use precompiled, simple regex without nested quantifiers to avoid ReDoS.
            var currentUrl = String(document.URL || '');
            this.set('lessonUrl', currentUrl.replace(LESSON_URL_REPLACE_REGEX, '.lesson'));

            if (LESSON_PAGE_NUM_REGEX.test(currentUrl)) {
                this.set('pageNum', currentUrl.replace(LESSON_PAGE_NUM_REGEX, '$1'));
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
