package org.grails.plugin.resource

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin

import org.grails.plugin.resource.module.*
import org.junit.Before
import spock.lang.Shared
import spock.lang.Specification

@TestMixin(GrailsUnitTestMixin)
class ResourceModuleSpec extends Specification {
    @Shared
    def svc
    
    @Before
    void setupTest() {
        svc = new Expando()
        svc.getDefaultSettingsForURI = { uri, type ->
            [:]
        }
    }
    
    void testDefaultBundleFalse() {
        when:
            def resources = [
                [url:'simile/simile.css'],
                [url:'simile/simile.js']
            ]

            def m = new ResourceModule('testModule', resources, false, svc)

        then:
            2 == m.resources.size()
            m.resources.every { it.bundle == null }
    }

    void testDefaultBundling() {
        when:
            def resources = [
                [url:'simile/simile.css', disposition:'head'],
                [url:'simile/simile.js', disposition:'head']
            ]

            def m = new ResourceModule('testModule', resources, null, svc)

        then:
            2 == m.resources.size()
            m.resources.each { r ->
                'bundle_testModule_head' == r.bundle
            }
    }

    void testDefaultBundleWithName() {
        when:
            def resources = [
                [url:'simile/simile.css', disposition:'defer'],
                [url:'simile/simile.js', disposition:'defer']
            ]

            def m = new ResourceModule('testModule', resources, "frank-and-beans", svc)

        then:
            2 == m.resources.size()
            m.resources.each { r ->
                'frank-and-beans_defer' == r.bundle
            }
    }

    void testExcludedMapperString() {
        when:
            def resources = [
                [url:'simile/simile.js', disposition:'head', exclude:'minify']
            ]

            def m = new ResourceModule('testModule', resources, null, svc)

        then:
            1 == m.resources.size()
            true == m.resources[0].excludedMappers.contains('minify')
    }

    void testExcludedMapperSet() {
        when:
            def resources = [
                [url:'simile/simile.js', disposition:'head', exclude:['minify']]
            ]

            def m = new ResourceModule('testModule', resources, null, svc)

        then:
            1 == m.resources.size()
            true == m.resources[0].excludedMappers.contains('minify')
    }

    void testStringOnlyResource() {
        when:
            def resources = [
                'js/test.js'
            ]

            def m = new ResourceModule('testModule', resources, null, svc)

        then:
            1 == m.resources.size()
            "/js/test.js" == m.resources[0].sourceUrl
    }

}