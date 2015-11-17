package org.grails.plugin.resource

import grails.test.mixin.TestFor

import org.grails.plugin.resource.util.HalfBakedLegacyLinkGenerator
import org.grails.taglib.GrailsTagException
import spock.lang.Specification

@TestFor(ResourceTagLib)
class ResourceTagLibSpec extends Specification {
	
    void setup() {
        Object.metaClass.encodeAsHTML = { -> delegate.toString() }
    }


    void testLinkResolutionForGrails2() {
        when:
            tagLib.grailsLinkGenerator = [
                resource: { attrs ->
                    "${attrs.contextPath}/${attrs.dir}/${attrs.file}"
                }
            ]
            tagLib.grailsResourceProcessor = [
                isDebugMode: { r -> false },
                getExistingResourceMeta: { uri ->
                    assert "/images/favicon.ico" == uri
                    def r = new ResourceMeta()
                    r.with {
                        sourceUrl = uri
                        actualUrl = uri
                    }
                    return r
                },
                staticUrlPrefix: '/static'
            ]

            tagLib.request.contextPath = "/CTX"

            def res = tagLib.resolveLinkUriToUriAndResource(dir:'images', file:'favicon.ico')

        then:
            "/CTX/static/images/favicon.ico" == res.uri
    }
    
    void testLinkResolutionForGrails2ResourceExcluded() {
        when:
            tagLib.grailsLinkGenerator = [
                resource: { attrs ->
                    "${attrs.contextPath}/${attrs.dir}/${attrs.file}"
                }
            ]

            tagLib.grailsResourceProcessor = [
                isDebugMode: { r -> false },
                getExistingResourceMeta: { uri ->
                    assert "/images/favicon.ico" == uri
                    return null // Excluded
                },
                getResourceMetaForURI: {uri,createAdHoc,declRes,postProc ->
                    assert "/images/favicon.ico" == uri
                    return null // Excluded
                },
                staticUrlPrefix: '/static'
            ]

            tagLib.request.contextPath = "/CTX"

            def res = tagLib.resolveLinkUriToUriAndResource(dir:'images', file:'favicon.ico')

        then:
            "/CTX/images/favicon.ico" == res.uri
    }
    
    void testLinkResolutionForGrails1_3AndEarlier() {
        when:
            tagLib.grailsLinkGenerator = new HalfBakedLegacyLinkGenerator()

            tagLib.grailsResourceProcessor = [
                isDebugMode: { r -> false },
                getExistingResourceMeta: { uri ->
                    assert "/images/favicon.ico" == uri
                    def r = new ResourceMeta()
                    r.with {
                        sourceUrl = uri
                        actualUrl = uri
                    }
                    return r
                },
                staticUrlPrefix: '/static'
            ]

            tagLib.request.contextPath = "/CTX"

            def res = tagLib.resolveLinkUriToUriAndResource(dir:'images', file:'favicon.ico')

        then:
            "/CTX/static/images/favicon.ico" == res.uri
    }
    
    void testAbsoluteDirFileLinkResolution() {
        when:
            tagLib.grailsResourceProcessor = [
                isDebugMode: { r -> false },
                getExistingResourceMeta: { uri ->
                    def r = new ResourceMeta()
                    r.with {
                        sourceUrl = uri
                        actualUrl = uri
                    }
                    return r
                },
                staticUrlPrefix: '/static'
            ]

            // We're just testing what happens if the link generator gave us back something absolute
            tagLib.request.contextPath = "/CTX"

            tagLib.grailsLinkGenerator = [resource: { args -> "http://myserver.com/CTX/static/"+args.dir+'/'+args.file } ]
            def res = tagLib.resolveLinkUriToUriAndResource(absolute:true, dir:'images', file:'default-avatar.png')

        then:
            "http://myserver.com/CTX/static/images/default-avatar.png" == res.uri
    }
    
    void testResourceLinkWithRelOverride() {
        when:
            def testMeta = new ResourceMeta()
            testMeta.sourceUrl = '/css/test.less'
            testMeta.actualUrl = '/css/test.less'
            testMeta.disposition = 'head'

            tagLib.grailsResourceProcessor = [
                isDebugMode: { r -> false },
                getExistingResourceMeta: { uri -> testMeta },
                staticUrlPrefix: '/static'
            ]
            def output = tagLib.external(uri:'/css/test.less', rel:'stylesheet/less', type:'css').toString()
            println "Output was: $output"

        then:
            output.contains('rel="stylesheet/less"')
            output.contains('href="/static/css/test.less"')
    }

    void testResourceLinkWithRelOverrideFromResourceDecl() {
        when:
            def testMeta = new ResourceMeta()
            testMeta.sourceUrl = '/css/test.less'
            testMeta.actualUrl = '/css/test.less'
            testMeta.contentType = "stylesheet/less"
            testMeta.disposition = 'head'
            testMeta.tagAttributes = [rel:'stylesheet/less']

            tagLib.grailsResourceProcessor = [
                isDebugMode: { r -> false },
                getExistingResourceMeta: { uri -> testMeta },
                staticUrlPrefix: '/static'
            ]
            def output = tagLib.external(uri:'/css/test.less', type:'css').toString()
            println "Output was: $output"

        then:
            output.contains('rel="stylesheet/less"')
            output.contains('href="/static/css/test.less"')
    }

    void testResourceLinkWithWrapperAttribute() {
        when:
            def testMeta = new ResourceMeta()
            testMeta.sourceUrl = '/css/ie.css'
            testMeta.actualUrl = '/css/ie.css'
            testMeta.contentType = "text/css"
            testMeta.disposition = 'head'
            testMeta.tagAttributes = [rel:'stylesheet']

            tagLib.grailsResourceProcessor = [
                isDebugMode: { r -> false },
                getExistingResourceMeta: { uri -> testMeta },
                staticUrlPrefix: '/static'
            ]
            def output = tagLib.external(uri:'/css/ie.less', type:'css', wrapper: { s -> "WRAPPED${s}WRAPPED" }).toString()
            println "Output was: $output"

        then:
            output.contains('rel="stylesheet"')
            assert !output.contains('wrapper='), "Should not contain the wrapper= attribute in output"
            output.contains('WRAPPED<link')
            output.contains('/>WRAPPED')
    }

    void testRenderModuleWithNonExistentResource() {
        when:
            def testMeta = new ResourceMeta()
            testMeta.sourceUrl = '/this/is/bull.css'
            testMeta.contentType = "test/stylesheet"
            testMeta.disposition = 'head'
            testMeta._resourceExists = false
            testMeta.tagAttributes = [rel:'stylesheet']

            def testMod = new ResourceModule()
            testMod.resources << testMeta

            tagLib.grailsResourceProcessor = [
                isDebugMode: { r -> false },
                getExistingResourceMeta: { uri -> testMeta },
                staticUrlPrefix: '/static',
                getModule : { name -> testMod }
            ]

        then:
            shouldFail(IllegalArgumentException) {
                def output = tagLib.renderModule(name:'test').toString()
            }
    }

    void testImgTagWithAttributes() {
        when:
            def testMeta = new ResourceMeta()
            testMeta.sourceUrl = '/images/test.png'
            testMeta.actualUrl = '/images/test.png'
            testMeta.contentType = "image/png"
            testMeta.disposition = 'head'
            testMeta.tagAttributes = [width:'100', height:'50', alt:'mugshot']

            tagLib.grailsResourceProcessor = [
                isDebugMode: { r -> false },
                getExistingResourceMeta: { uri -> testMeta },
                staticUrlPrefix: '/static'
            ]
            def output = tagLib.img(uri:'/images/test.png').toString()
            println "Output was: $output"

        then:
            output.contains('width="100"')
            output.contains('height="50"')
            output.contains('alt="mugshot"')
            output.contains('src="/static/images/test.png"')
            !output.contains('uri=')
    }

    void testImgTagWithAttributesDefaultDir() {
        when:
            def testMeta = new ResourceMeta()
            testMeta.sourceUrl = '/images/test.png'
            testMeta.actualUrl = '/images/test.png'
            testMeta.contentType = "image/png"
            testMeta.disposition = 'head'
            testMeta.tagAttributes = [width:'100', height:'50', alt:'mugshot']

            tagLib.grailsResourceProcessor = [
                isDebugMode: { r -> false },
                getExistingResourceMeta: { uri -> testMeta },
                staticUrlPrefix: '/static'
            ]

            tagLib.grailsLinkGenerator = [
                resource: {attrs ->
                    assert 'test.png' == attrs.file
                    assert 'images' == attrs.dir

                    return '/images/test.png'
                }
            ]

            def output = tagLib.img(file:'test.png').toString()
            println "Output was: $output"

        then:
            output.contains('width="100"')
            output.contains('height="50"')
            output.contains('src="/static/images/test.png"')
            !output.contains('file=')
            !output.contains('dir=')
    }

    void testDebugModeResourceLinkWithAbsoluteCDNURL() {
        when:
            def url = 'https://ajax.googleapis.com/ajax/libs/jquery/1.4/jquery.min.js'
            def testMeta = new ResourceMeta()
            testMeta.sourceUrl = url
            testMeta.actualUrl = url
            testMeta.disposition = 'head'

            tagLib.request.contextPath = "/resourcestests"

            tagLib.grailsResourceProcessor = [
                isDebugMode: { r -> true },
                getExistingResourceMeta: { uri -> testMeta },
                staticUrlPrefix: '/static'
            ]
            def output = tagLib.external(uri:url, type:"js").toString()
            println "Output was: $output"

        then:
            output.contains('src="https://ajax.googleapis.com/ajax/libs/jquery/1.4/jquery.min.js?_debugResources')
    }
    
    void testLinkToAbsoluteResourceWithQueryParams() {
        when:
            def url = 'https://ajax.googleapis.com/ajax/libs/jquery/1.4/jquery.min.js?x=y#nasty'
            def testMeta = new ResourceMeta()
            testMeta.sourceUrl = url
            testMeta.actualUrl = url
            testMeta._linkUrl = url
            testMeta.disposition = 'head'

            tagLib.request.contextPath = "/resourcestests"

            tagLib.grailsResourceProcessor = [
                isDebugMode: { r -> false },
                getExistingResourceMeta: { uri -> testMeta },
                staticUrlPrefix: '/static'
            ]
            def output = tagLib.external(uri:url, type:"js").toString()
            println "Output was: $output"

        then:
            output.contains('src="'+url+'"')
    }
	
	void testLinkToGoogleFontCssWithComplexQuery() {
        when:
            def url = 'http://fonts.googleapis.com/css?family=PT+Sans:400,700&subset=latin,cyrillic'

            // setup ResourceMeta which will always be processed
            def testMeta = new ResourceMeta()
            // original url, verbatim
            testMeta.originalUrl = url
            // source is app relative url of source, minus the query params
            testMeta.sourceUrl = 'http://fonts.googleapis.com/css'
            // actual url is relative url after processing, minus the query params
            // null in this case because the link is absolute
            testMeta.actualUrl = null
            // link url itself, without override
            testMeta._linkUrl = url

            testMeta.disposition = 'head'

            tagLib.request.contextPath = "/resourcestests"

            tagLib.grailsResourceProcessor = [
                isDebugMode: { r -> false },
                getExistingResourceMeta: { uri -> testMeta },
                staticUrlPrefix: '/static'
            ]

            def output = tagLib.external(uri:url, type:"css").toString()
            println "Output was: $output"

        then:
		    output.contains('href="'+url+'"')
	}
    
    void testRequireUpdatesRequestAttributes() {
        when:
            tagLib.grailsResourceProcessor = [
                addModuleDispositionsToRequest: { req, module -> }
            ]

            def output = tagLib.require(modules:['thingOne', 'thingTwo']).toString()

            def tracker = tagLib.request.resourceModuleTracker

        then:
            null != tracker
            2 == tracker?.size()
            tracker.containsKey('thingOne')
            true == tracker.thingOne
            tracker.containsKey('thingTwo')
            true == tracker.thingOne

            // We must never include the ADHOC or SYNTHETIC modules!
            !tracker.containsKey(ResourceProcessor.ADHOC_MODULE)
            !tracker.containsKey(ResourceProcessor.SYNTHETIC_MODULE)
    }
    
    void testRequireIndicatesModuleNotMandatory() {
        when:
            tagLib.grailsResourceProcessor = [
                addModuleDispositionsToRequest: { req, module -> }
            ]

            def output = tagLib.require(modules:['thingOne', 'thingTwo'], strict:false).toString()

            def tracker = tagLib.request.resourceModuleTracker

        then:
            null != tracker
            2 == tracker?.size()
            tracker.containsKey('thingOne')
            false == tracker.thingOne
            tracker.containsKey('thingTwo')
            false == tracker.thingTwo

            // We must never include the ADHOC or SYNTHETIC modules!
            !tracker.containsKey(ResourceProcessor.ADHOC_MODULE)
            !tracker.containsKey(ResourceProcessor.SYNTHETIC_MODULE)
    }

    void testExternalTagCanWorkWithUrlUriOrDir() {
        when:
            tagLib.external(uri: '/fake/url')
            tagLib.external(url: '/fake/url')
            tagLib.external(file: 'myfile.js')

        then: 'We should allow the tag to be used with any of the above attributes present'
            Object e = thrown()
            assert e instanceof GrailsTagException
            assert !(e instanceof Exception) // We expect this because the rest of the tag isn't mocked.
    }

    void testExternalTagRequiresUrlUriOrDir() {
        when:
            tagLib.external([:])

        then: 'Should have thrown an exception due to missing required attributes'
            Exception e = thrown()
            e.message == 'For the &lt;r:external /&gt; tag, one of the attributes [uri, url, file] must be present'
    }
}
