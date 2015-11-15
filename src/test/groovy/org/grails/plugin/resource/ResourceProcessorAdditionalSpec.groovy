package org.grails.plugin.resource

import org.junit.Before
import spock.lang.Shared
import spock.lang.Specification

import static org.junit.Assert.*;
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin

import javax.servlet.ServletContext

import org.grails.plugins.testing.GrailsMockHttpServletRequest
import org.grails.plugins.testing.GrailsMockHttpServletResponse
import org.junit.Rule
import org.junit.Test;
import org.junit.rules.TemporaryFolder

@TestMixin(GrailsUnitTestMixin)
class ResourceProcessorAdditionalSpec extends Specification {
    @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Shared
    File temporarySubfolder
    @Shared
    ResourceProcessor processor

    @Before
    void setupTest() {
        //mockLogging(ResourceProcessor, true)
        temporarySubfolder = temporaryFolder.newFolder('test-tmp')
        processor = new ResourceProcessor()
        
        def servletContext = [
                getResource: { uri -> 
                    assertTrue uri.indexOf('#') < 0
                    new URL('file:./test/test-files'+uri) 
                },
                getMimeType: { uri -> "test/nothing" }
            ] as ServletContext
        processor.grailsApplication = [
            config : [grails:[resources:[work:[dir:temporarySubfolder.getAbsolutePath()]]]],
            mainContext : [servletContext:servletContext]
        ]
        processor.servletContext = servletContext
        processor.afterPropertiesSet()
        processor.adHocIncludes += '/somehack.xml'
    }

    @Test
    void testPrepareURIWithHashFragment() {
        when:
            def r = new ResourceMeta()
            r.sourceUrl = '/somehack.xml#whatever'

            def meta = processor.prepareResource(r, true)

        then:
            null != meta
            '/somehack.xml' == meta.actualUrl
            '/somehack.xml#whatever' == meta.linkUrl
    }

    @Test
    void testPrepareAbsoluteURLWithQueryParams() {
        when:
            def r = new ResourceMeta()
            r.sourceUrl = 'http://crackhouse.ck/css/somehack.css?x=y#whatever'

            def meta = processor.prepareResource(r, true)

        then:
            null != meta
            'http://crackhouse.ck/css/somehack.css' == meta.actualUrl
            'http://crackhouse.ck/css/somehack.css?x=y#whatever' == meta.linkUrl
    }

    // GRESOURCES-116
    @Test
    void testPrepareAbsoluteURLWithMissingExtension() {
        when:
            def r = new ResourceMeta()
            r.workDir = new File('/tmp/test')
            r.sourceUrl = 'http://maps.google.com/maps/api/js?v=3.5&sensor=false'
            r.disposition = 'head'
            r.tagAttributes = [type: 'js']

            ResourceMeta meta = processor.prepareResource(r, true)

        then:
            null != meta
            'http://maps.google.com/maps/api/js' == meta.actualUrl
            'http://maps.google.com/maps/api/js?v=3.5&sensor=false' == meta.linkUrl
            [type: 'js']. equals (meta.tagAttributes)
    }

    @Test
    void testBuildResourceURIForGrails1_4() {
        when:
            def r = new ResourceMeta()
            r.sourceUrl = '/somehack.xml#whatever'

            def meta = processor.prepareResource(r, true)

        then:
            null != meta
            '/somehack.xml' == meta.actualUrl
            '/somehack.xml#whatever' == meta.linkUrl
    }

    @Test
    void testBuildResourceURIForGrails1_3AndLower() {
        when:
            def r = new ResourceMeta()
            r.sourceUrl = '/somehack.xml#whatever'

            def meta = processor.prepareResource(r, true)

        then:
            null != meta
            '/somehack.xml' == meta.actualUrl
            '/somehack.xml#whatever' == meta.linkUrl
    }

    @Test
    void testProcessLegacyResourceIncludesExcludes() {

        when:
            processor.adHocIncludes = ['/**/*.css', '/**/*.js', '/images/**']
            processor.adHocExcludesLowerCase = ['/**/*.exe', '/**/*.gz', '/unsafe/**/*.css']

            def testData = [
                [requestURI: '/css/main.css', expected:true],
                [requestURI: '/js/code.js', expected:true],
                [requestURI: '/css/logo.png', expected:false],
                [requestURI: '/images/logo.png', expected:true],
                [requestURI: '/downloads/virus.exe', expected:false],
                [requestURI: '/downloads/archive.tar.gz', expected:false],
                [requestURI: '/unsafe/nested/problematic.css', expected:false]
            ]

        then:
            testData.each { d ->
                def request = [contextPath:'resources', requestURI: 'resources'+d.requestURI]

                // We know if it tried to handle it if it 404s, we can't be bothered to creat resourcemeta for all those
                def didHandle = false
                def response = [
                    sendError: { code, msg = null -> didHandle = (code == 404) },
                    sendRedirect: { uri -> }
                ]

                processor.processLegacyResource(request, response)

                assert d.expected == didHandle, "Failed on ${d.requestURI}"
            }
    }

    @Test
    void testProcessLegacyResourceIncludesExcludesSpecificFile() {

        when:
            processor.adHocIncludes = ['/**/*.js']
            processor.adHocExcludesLowerCase = ['/**/js/something.js']

            def testData = [
                [requestURI: '/js/other.js', expected:true],
                [requestURI: '/js/something.js', expected:false],
                [requestURI: 'js/something.js', expected:false],
                [requestURI: '/xxx/js/something.js', expected:false],
                [requestURI: 'xxx/js/something.js', expected:false]
            ]

        then:
            testData.each { d ->
                def request = [contextPath:'resources', requestURI: 'resources'+d.requestURI]

                // We know if it tried to handle it if it 404s, we can't be bothered to create resourcemeta for all those
                def didHandle = false
                def response = [
                    sendError: { code, msg = null -> didHandle = true },
                    sendRedirect: { uri -> }
                ]

                processor.processLegacyResource(request, response)

                assert d.expected == didHandle, "Failed on ${d.requestURI}"
            }
    }
    
    @Test
    void testDependencyOrdering() {
        when:
            processor.modulesByName = [
                a: [name:'a', dependsOn:['b']],
                e: [name:'e', dependsOn:['f', 'a']],
                b: [name:'b', dependsOn:['c']],
                c: [name:'c', dependsOn:['q']],
                d: [name:'d', dependsOn:['b', 'c']],
                f: [name:'f', dependsOn:['d']],
                z: [name:'z', dependsOn:[]],
                q: [name:'q', dependsOn:[]]
            ]
            processor.updateDependencyOrder()

            def res = processor.modulesInDependencyOrder
            def pos = { v ->
                res.indexOf(v)
            }

            println "Dependency order: ${res}"

        then:
            res.size()-2 == processor.modulesByName.keySet().size() // take off the synth + adhoc

            pos('a') > pos('b')

            pos('e') > pos('f')
            pos('e') > pos('a')

            pos('b') > pos('c')

            pos('c') > pos('q')

            pos('d') > pos('b')
            pos('d') > pos('c')

            pos('f') > pos('d')
    }
    
    @Test
    void testWillNot404OnAdhocResourceWhenAccessedDirectlyFromStaticUrl() {
        when:
            processor.adHocIncludes = ["/**/*.xml"]
            processor.staticUrlPrefix = "/static"

            GrailsMockHttpServletRequest request = new GrailsMockHttpServletRequest()
            request.contextPath = "resources"
            request.requestURI = "resources/static/somehack.xml"

            GrailsMockHttpServletResponse response = new GrailsMockHttpServletResponse()

            processor.processModernResource(request, response)

        then:
            response.redirectedUrl == null
            response.contentAsString.size() > 0
            response.contentLength > 0
    }

    @Test
    void testRedirectToActualUrlWithAbsoluteLinkUrlRedirectedToThatUrl() {
        when:
            processor.staticUrlPrefix = "/static"

            String absoluteURL = "http://absolute.example.org/url/to/images/test/resource.png"
            ResourceMeta resourceMeta = new ResourceMeta()
            resourceMeta.linkOverride = absoluteURL
            GrailsMockHttpServletRequest request = new GrailsMockHttpServletRequest()
            GrailsMockHttpServletResponse response = new GrailsMockHttpServletResponse()

            request.contextPath = "/custom-context-path"
            request.requestURI = "/images/test/resource.png"

            processor.redirectToActualUrl(resourceMeta, request, response)

        then:
            response.redirectedUrl == absoluteURL
    }
}
