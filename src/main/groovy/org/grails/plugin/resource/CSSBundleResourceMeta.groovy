package org.grails.plugin.resource

import groovy.util.logging.Slf4j

/**
 * Holder for info about a resource that is made up of other resources
 *
 * @author Marc Palmer (marc@grailsrocks.com)
 * @author Luke Daley (ld@ldaley.com)
 */
@Slf4j
class CSSBundleResourceMeta extends AggregatedResourceMeta {

    @Override
    void beginPrepare(grailsResourceProcessor) {
        initFile(grailsResourceProcessor)
        
        def out = getWriter()
        out << '@charset "UTF-8";\n'
        out.close()

        buildAggregateResource(grailsResourceProcessor)
    }
}