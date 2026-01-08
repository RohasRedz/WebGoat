define(['jquery', 'underscore', 'backbone', 'goatApp/model/AssignmentModel'], function(
  $, _, Backbone, AssignmentModel,
) {
  'use strict';

  var docsModel = Backbone.Model.extend({
    defaults: {
      name: null,
      category: null,
      assignment: AssignmentModel,
      description: String,
      tip: String,
      source: String,
      rationale: String,
      risk: String,
      fix: String,
      classification: String,
    },

    sync: function(method, model, options) {
      options = options || {};
      options.type = 'GET';
      options.dataType = 'json';
      options.contentType = 'application/json';

      // Security hardening: enforce safe, bounded URL patterns if a custom URL is provided.
      // This prevents potential ReDoS or unexpected behavior from overly complex or untrusted URLs.
      if (options.url && typeof options.url === 'string') {
        // Allow only relative paths starting with / or ./ and having a reasonable length.
        var MAX_URL_LENGTH = 2048;
        if (
          options.url.length > MAX_URL_LENGTH ||
          !/^(\/|\.\/)[A-Za-z0-9/_\-.]*$/.test(options.url)
        ) {
          throw new Error('Invalid or unsafe URL provided to LessonContentModel.sync');
        }
      }

      return Backbone.sync(method, model, options);
    },

  });

  return docsModel;
});
