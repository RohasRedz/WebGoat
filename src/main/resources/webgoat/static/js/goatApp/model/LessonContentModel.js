define([
    'jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel'
], function (
    $,
    _,
    Backbone,
    HTMLContentModel
) {

    return HTMLContentModel.extend({
        urlRoot: null,
        defaults: {
            items: null,
            selectedItem: null
        },

        initialize: function (options) {
            // no-op
        },

        loadData: function (options) {
            // Ensure name is a reasonably bounded, string input
            var name = (options && typeof options.name === 'string') ? options.name : '';
            // Limit length to mitigate potential abuse
            if (name.length > 200) {
                name = name.substring(0, 200);
            }
            this.urlRoot = _.escape(encodeURIComponent(name)) + '.lesson';
            var self = this;
            this.fetch().done(function (data) {
                self.setContent(data);
            });
        },

        setContent: function (content, loadHelps) {
            if (typeof loadHelps === 'undefined') {
                loadHelps = true;
            }
            this.set('content', content);

            // Precompile and reuse a safe, bounded regex for URL manipulation.
            // This avoids excessive backtracking and keeps complexity predictable.
            var lessonUrlPattern = /\.lesson(?:\/(\d{1,4}))?$/;
            var currentUrl = document.URL;

            // Normalize URL to a string and enforce a sane maximum length to avoid ReDoS-style abuse
            if (typeof currentUrl !== 'string') {
                currentUrl = String(currentUrl);
            }
            if (currentUrl.length > 2000) {
                currentUrl = currentUrl.substring(0, 2000);
            }

            // Derive the base lesson URL safely
            this.set('lessonUrl', currentUrl.replace(lessonUrlPattern, '.lesson'));

            var match = currentUrl.match(lessonUrlPattern);
            if (match && match[1]) {
                this.set('pageNum', match[1]);
            } else {
                this.set('pageNum', 0);
            }

            this.trigger('content:loaded', this, loadHelps);
        },

        fetch: function (options) {
            options = options || {};
            return Backbone.Model.prototype.fetch.call(
                this,
                _.extend({ dataType: 'html' }, options)
            );
        }
    });
});
