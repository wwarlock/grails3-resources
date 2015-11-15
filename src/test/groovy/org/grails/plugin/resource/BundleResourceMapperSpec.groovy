package org.grails.plugin.resource

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Shared
import spock.lang.Specification;

public @TestMixin(GrailsUnitTestMixin)
class BundleResourceMapperSpec extends Specification {

    @Shared
    BundleResourceMapper mapper = new BundleResourceMapper()

    void testRecognisedArtifactsAreBundledIfRequested() {

        when:
            Map<String, Boolean> expectedBundling = [
                'text/css': true,
                'text/javascript': true,
                'application/javascript': true,
                'application/x-javascript': true,
                'everything/nothing': false
            ]

            List resultingBundle
            Map grailsResourceProcessor = [
                    findSyntheticResourceById: { String bundleId -> return resultingBundle },
            ]

            mapper.grailsResourceProcessor = grailsResourceProcessor

        then:
            expectedBundling.each { String resourceType, Boolean shouldBundle ->
                resultingBundle = [[existing:'bundle']]
                Map resource = [identifier:UUID.randomUUID(), contentType: resourceType, bundle:'myBundle', sourceUrlExtension:'js']

                mapper.map(resource, null)

                (resultingBundle[1]?.identifier == resource.identifier) == shouldBundle
            }

    }

}
