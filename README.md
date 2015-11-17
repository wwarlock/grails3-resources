Tried to convert old [Grails 2 plugin](https://github.com/grails-plugins/grails-resources) to the Grails 3 format.
With partial succes for now.
It compiles successfully but some tests are red.

***WARNING !!! That is broken plugin. It DOES NOT work as expected.***

Please do not use it in production.
But if you can fix the code, you're welcome.


[![Build Status](https://api.travis-ci.org/gpc/grails-mail.png)](https://travis-ci.org/wwarlock/grails3-resources)

Grails Resources framework.
===========================

Grails resources provides a DSL for declaring static resources in plugins and in apps, and a mapper artefact that allows other plugins to provide processing of static resources to e.g. minify / zip them.

All processing is performed at runtime once, against files in the server filesystem.

Built-in mappers included in the plugin:

* CSS rewriter (two mappers required, happens automatically)
* Bundler (combines multiple css or js files into one)

See docs at http://grails.org/plugin/resources
