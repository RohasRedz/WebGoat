define(['jquery',
    'underscore',
    'backbone',
    'goatApp/model/HTMLContentModel'],
     function($,
        _,
        Backbone,
        HTMLContentModel){

    // Limit lesson name to safe characters and reasonable length to avoid ReDoS and malformed URLs
    function sanitizeLessonName(name) {
        if (typeof name !== 'string') {
            return '';
        }
        // Trim and limit length
        var trimmed = name.trim().slice(0, 128);
        // Allow only letters, numbers, underscore, dash and dot to keep URLs simple and safe
        var safe = trimmed.replace(/[^a-zA-Z0-9._-]/g, '');
        return safe;
    }

    // Derive a safe lesson URL from document.URL without using complex regular expressions
    function deriveLessonUrl(currentUrl) {
        if (typeof currentUrl !== 'string') {
            return '';
        }
        // Remove any query string or fragment
        var baseUrl = currentUrl.split('#')[0].split('?')[0];
        // If the URL ends with `.lesson` or `.lesson/`, normalize it
        if (baseUrl.indexOf('.lesson') !== -1) {
            // Keep only up to `.lesson`
            var index = baseUrl.indexOf('.lesson');
            return baseUrl.substring(0, index + '.lesson'.length);
        }
        return baseUrl;
    }

    // Derive a numeric page number without using backtrackingprone regexes
    function derivePageNum(currentUrl) {
        if (typeof currentUrl !== 'string') {
            return 0;
        }
        // Simple, fast parsing:
        // 1. Strip query/fragment
        var baseUrl = currentUrl.split('#')[0].split('?')[0];
        // 2. Split by '/'
        var segments = baseUrl.split('/');
        var lastSegment = segments[segments.length - 1] || '';
        // Expect formats like `something.lesson/12` or `something.lesson`
        var parts = lastSegment.split('.');
        // If last segment does not contain `.lesson`, return 0
        if (parts.indexOf('lesson') === -1) {
            return 0;
        }
        // Page number may be in the previous path segment (e.g. /foo.lesson/12)
        // or encoded in the last segment after `lesson/`
        // So also check the last numeric-looking segment in the whole URL
        var possibleSegments = segments.slice().reverse();
        for (var i = 0; i < possibleSegments.length; i++) {
            var seg = possibleSegments[i];
            if (/^\d{1,4}$/.test(seg)) {
                return parseInt(seg, 10);
            }
        }
        return 0;
    }

    return HTMLContentModel.extend({
        urlRoot:null,
        defaults: {
            items:null,
            selectedItem:null
        },

        initialize: function (options) {

        },

        loadData: function(options) {
            // Sanitize lesson name instead of directly encoding arbitrary input
            var safeName = sanitizeLessonName(options && options.name ? options.name : '');
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

            var currentUrl = document.URL || '';

            // Use safer helper to derive lesson URL without complex regex
            var safeLessonUrl = deriveLessonUrl(currentUrl);
            this.set('lessonUrl', safeLessonUrl);

            // Use helper to derive a bounded numeric page number
            var pageNum = derivePageNum(currentUrl);
            this.set('pageNum', pageNum);

            this.trigger('content:loaded',this,loadHelps);
        },

        fetch: function (options) {
            options = options || {};
            return Backbone.Model.prototype.fetch.call(this, _.extend({ dataType: "html"}, options));
        }
    });
});
