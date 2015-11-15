package org.grails.plugin.resource

import grails.core.GrailsApplication
import grails.test.mixin.integration.Integration
import org.grails.plugins.testing.GrailsMockHttpServletRequest
import org.grails.plugins.testing.GrailsMockHttpServletResponse
import spock.lang.Specification

@Integration
class LegacyResourceIntegrationSpec extends Specification {
    
    ResourceProcessor grailsResourceProcessor
    GrailsApplication grailsApplication
    
    // GPRESOURCES-214
    def 'legacy resource with baseurl'() {
        grailsApplication.config.grails.resources.mappers.baseurl.enabled = true
        grailsApplication.config.grails.resources.mappers.baseurl.default = "http://cdn.domain.com/static"

        GrailsMockHttpServletRequest request = new GrailsMockHttpServletRequest()
        GrailsMockHttpServletResponse response = new GrailsMockHttpServletResponse()
        
        request.requestURI = "/images/springsource.png"
        
        when:
            grailsResourceProcessor.processLegacyResource(request, response)

        then:
            response.redirectedUrl == "http://cdn.domain.com/static/images/_springsource.png"
    }
    
}
