define([
    'jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel'
], function ($, _, Backbone, HTMLContentModel) {

    return HTMLContentModel.extend({
        urlRoot: null,
        defaults: {
            items: null,
            selectedItem: null
        },

        initialize: function (options) {
            // no-op initializer
        },

        loadData: function (options) {
            // Ensure options and options.name are well-defined strings before encoding
            var name = options && typeof options.name === 'string' ? options.name : '';

            // First encode, then escape the encoded value to avoid double-encoding issues
            var safeName = _.escape(encodeURIComponent(name));
            this.urlRoot = safeName + '.lesson';

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

            // Use location.href to be explicit and work with full URL; avoid overly broad regex
            var currentUrl = String(window.location && window.location.href ? window.location.href : '');

            // Derive lessonUrl safely: replace trailing ".lesson" or ".lesson/<page>" with ".lesson"
            this.set('lessonUrl', currentUrl.replace(/\.lesson(?:\/\d{1,4})?$/, '.lesson'));

            // Extract page number only from a strict ".lesson/<1-4 digits>" suffix to
            // limit regex work and avoid inefficient patterns on arbitrary strings
            var pageMatch = currentUrl.match(/\.lesson\/(\d{1,4})$/);
            if (pageMatch) {
                this.set('pageNum', pageMatch[1]);
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
