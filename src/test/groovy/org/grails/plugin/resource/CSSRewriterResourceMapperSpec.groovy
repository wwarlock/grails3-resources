package org.grails.plugin.resource
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification

@TestMixin(GrailsUnitTestMixin)
class CSSRewriterResourceMapperSpec extends Specification {
    @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder()

    @Shared
    File temporarySubfolder
    
    void setup() {
        temporarySubfolder = temporaryFolder.newFolder('test-tmp')
        //mockLogging(org.grails.plugin.resource.CSSRewriterResourceMapper)
    }

    /**
     * This simulates a test where the image resources are moved to a new flat dir
     * but the CSS is *not* moved, to force recalculation of paths
     */
    void testCSSRewritingWithMovingFiles() {

        when:
            def r = new ResourceMeta(sourceUrl:'/css/main.css')

            def svc = [
                getResourceMetaForURI : {  uri, adHoc, declRes, postProc = null ->
                    def namepart = uri[uri.lastIndexOf('/')..-1]
                    def s = '/cached'+namepart
                    def newRes = new ResourceMeta(actualUrl: s)
                    r.declaringResource = declRes
                    if (postProc) postProc(newRes)
                    assert r.sourceUrl == declRes, 'CSS rewriter did not set declaring resource correctly'
                    return newRes
                },
                config : [ rewrite: [css: true] ]
            ]

            r.workDir = temporarySubfolder
            r.actualUrl = r.sourceUrl
            r.contentType = "text/css"
            r.processedFile = new File(temporarySubfolder, 'css/main.css')
            r.processedFile.parentFile.mkdirs()
            r.processedFile.delete()

            def css = """
.bg1 { background: url(resource:/images/theme/bg1.png) }
.bg2 { background: url(resource:/images/bg2.png) }
.bg3 { background: url(resource:/images/bg3.png) }
.bg4 { background: url(resource:/bg4.png) }
"""
            r.processedFile << new ByteArrayInputStream(css.bytes)

            org.grails.plugin.resource.CSSRewriterResourceMapper.newInstance().with {
                grailsResourceProcessor = svc
                map(r, new ConfigObject())
            }

            def outcome = r.processedFile.text
            def expected = """
.bg1 { background: url(../cached/bg1.png) }
.bg2 { background: url(../cached/bg2.png) }
.bg3 { background: url(../cached/bg3.png) }
.bg4 { background: url(../cached/bg4.png) }
"""

        then:
            expected == outcome
    }

    /**
     * This simulates a mapping where the image resources are renamed but left in the same location,
     * and the actualUrl is not mutated (i.e. like zipping)
     */
    void testCSSRewritingWithRenamedFilesBySameUrl() {

        when:
            def svc = [
                getResourceMetaForURI : {  uri, adHoc, declRes, postProc = null ->
                    new ResourceMeta(actualUrl: uri, processedFile: new File(uri+'.gz'))
                },
                config : [ rewrite: [css: true] ]
            ]

            def r = new ResourceMeta(sourceUrl:'/css/main.css')
            r.workDir = temporarySubfolder
            r.actualUrl = r.sourceUrl
            r.contentType = 'text/css'
            r.processedFile = new File(temporarySubfolder, 'css/main.css')
            r.processedFile.parentFile.mkdirs()
            r.processedFile.delete()

            def css = """
.bg1 { background: url(resource:/images/theme/bg1.png) }
.bg2 { background: url(resource:/images/bg2.png) }
.bg3 { background: url(resource:/images/bg3.png) }
.bg4 { background: url(resource:/bg4.png) }
"""
            r.processedFile << new ByteArrayInputStream(css.bytes)

            org.grails.plugin.resource.CSSRewriterResourceMapper.newInstance().with {
                grailsResourceProcessor = svc
                map(r, new ConfigObject())
            }

            def outcome = r.processedFile.text
            def expected = """
.bg1 { background: url(../images/theme/bg1.png) }
.bg2 { background: url(../images/bg2.png) }
.bg3 { background: url(../images/bg3.png) }
.bg4 { background: url(../bg4.png) }
"""

        then:
            expected == outcome
    }

    /**
     * This simulates CSS that uses some MS IE css behaviour hacks that can cause problems
     * as they are not valid URLs
     */
    void testCSSRewritingWithInvalidURI() {

        when:
            def svc = [
                getResourceMetaForURI : {  uri, adHoc, declRes, postProc = null ->
                    new ResourceMeta(actualUrl: uri, processedFile: new File(uri+'.gz'))
                },
                config : [ rewrite: [css: true] ]
            ]

            def r = new ResourceMeta(sourceUrl:'/css/main.css')
            r.workDir = temporarySubfolder
            r.actualUrl = r.sourceUrl
            r.contentType = 'text/css'
            r.processedFile = new File(temporarySubfolder, 'css/main.css')
            r.processedFile.parentFile.mkdirs()
            r.processedFile.delete()

            def css = """
.bg1 { behaviour: url(#default#VML) }
.bg2 { background: url(####BULL) }
"""
            r.processedFile << new ByteArrayInputStream(css.bytes)

            org.grails.plugin.resource.CSSRewriterResourceMapper.newInstance().with {
                grailsResourceProcessor = svc
                map(r, new ConfigObject())
            }

            def outcome = r.processedFile.text
            def expected = """
.bg1 { behaviour: url(#default#VML) }
.bg2 { background: url(####BULL) }
"""

        then:
            expected == outcome
    }

    /**
     * This simulates CSS that uses some MS IE css behaviour hacks that can cause problems
     * as they are not valid URLs
     */
    void testCSSRewritingWithQueryParamsAndFragment() {

        when:
            def svc = [
                getResourceMetaForURI : {  uri, adHoc, declRes, postProc = null ->
                    def r = new ResourceMeta()
                    r.sourceUrl = uri
                    r.actualUrl = r.sourceUrl
                    r.actualUrl += '.gz' // frig it as if a mapper changed it
                    return r
                },
                config : [ rewrite: [css: true] ],
                getResource: { uri ->
                    new URL('file:./test/test-files'+uri)
                },
                getMimeType: { uri -> "test/nothing" }
            ]

            def r = new ResourceMeta(sourceUrl:'/css/main.css')
            r.workDir = temporarySubfolder
            r.actualUrl = r.sourceUrl
            r.contentType = 'text/css'
            r.processedFile = new File(temporarySubfolder, 'css/main.css')
            r.processedFile.parentFile.mkdirs()
            r.processedFile.delete()

            def css = """
.bg1 { behaviour: url(resource:/image.png?arg1=value1) }
.bg2 { background: url(resource:/image.png#bogus-but-what-the-hell) }
"""
            r.processedFile << new ByteArrayInputStream(css.bytes)

            org.grails.plugin.resource.CSSRewriterResourceMapper.newInstance().with {
                grailsResourceProcessor = svc
                map(r, new ConfigObject())
            }

            def outcome = r.processedFile.text

            println "Output: $outcome"
            def expected = """
.bg1 { behaviour: url(../image.png.gz?arg1=value1) }
.bg2 { background: url(../image.png.gz#bogus-but-what-the-hell) }
"""

        then:
            expected == outcome
    }

    void testCSSRewritingWithAbsoluteLinkOverride() {

        when:
            def svc = [
                getResourceMetaForURI : {  uri, adHoc, declRes, postProc = null ->
                    def r = new ResourceMeta()
                    r.sourceUrl = uri
                    r.actualUrl = "http://mycdn.somewhere.com/myresources/x.jpg"
                    return r
                },
                config : [ rewrite: [css: true] ],
                getResource: { uri ->
                    new URL('file:./test/test-files'+uri)
                },
                getMimeType: { uri -> "test/nothing" }
            ]

            def r = new ResourceMeta(sourceUrl:'/css/main.css')
            r.workDir = temporarySubfolder
            r.actualUrl = r.sourceUrl
            r.contentType = 'text/css'
            r.processedFile = new File(temporarySubfolder, 'css/main.css')
            r.processedFile.parentFile.mkdirs()
            r.processedFile.delete()

            def css = """
.bg1 { background: url(resource:/image.png) }
"""
            r.processedFile << new ByteArrayInputStream(css.bytes)

            org.grails.plugin.resource.CSSRewriterResourceMapper.newInstance().with {
                grailsResourceProcessor = svc
                map(r, new ConfigObject())
            }

            def outcome = r.processedFile.text

            println "Output: $outcome"
            def expected = """
.bg1 { background: url(http://mycdn.somewhere.com/myresources/x.jpg) }
"""

        then:
            expected == outcome
    }

}