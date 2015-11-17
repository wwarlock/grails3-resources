package org.grails.plugin.resource

import grails.test.mixin.integration.Integration
import grails.test.mixin.web.GroovyPageUnitTestMixin
import grails.test.mixin.TestMixin
import org.grails.plugin.resources.stash.StashManager
import org.grails.plugin.resources.stash.StashWriter
import spock.lang.Specification

@Integration
@TestMixin(GroovyPageUnitTestMixin)
class ResourceTagLibIntegSpec extends Specification {
    def grailsResourceProcessor

    void setup() {
        // reload all modules with bundling applied to them
        grailsResourceProcessor.reloadAll()
    }

    void testExternalWithAbsoluteURI() {
        when:
            def result = applyTemplate('<r:external uri="https://ajax.googleapis.com/ajax/libs/jquery/1.4/jquery.min.js"/>', [:])

        then:
            result.indexOf('"https://ajax.googleapis.com/ajax/libs/jquery/1.4/jquery.min.js') != -1
    }

    void testExternalWithAdhocResourceURIThatIsExcluded() {
        when:
            def result = applyTemplate('<r:external uri="js/core.js"/>', [:])

        then:
            result.indexOf('/js/core.js') != -1
    }

    void testExternalWithAdhocResourceDirAndFileThatIsExcluded() {
        when:
            def result = applyTemplate('<r:external dir="js" file="core.js"/>', [:])

        then:
            result.indexOf('/js/core.js') != -1
    }

    void testExternalWithAdhocResourceURI() {
        when:
            def result = applyTemplate('<r:external uri="js/adhoc.js"/>', [:])

        then:
            result.indexOf('/static/js/_adhoc.js') != -1
    }

    void testExternalWithAdhocResourceURIWithSlash() {
        when:
            def result = applyTemplate('<r:external uri="/js/adhoc.js"/>', [:])

        then:
            result.indexOf('/static/js/_adhoc.js') != -1
    }

    void testExternalWithAdhocResourceDirAndFile() {
        when:
            def result = applyTemplate('<r:external dir="js" file="adhoc.js"/>', [:])

        then:
            result.indexOf('/static/js/_adhoc.js') != -1
    }

	void testGoogleFontsWithQueriesInModule() {
        when:
            def template = '''<html>
                                <head>
                                  <r:require modules="testurl"/>
                                  <r:layoutResources/>
                                </head>
                                <body>
                                  <h1>Hi</h1>
                                </body>
                              </html>'''
            def result = applyTemplate(template, [:])
            def expectedLink = '<link href="http://fonts.googleapis.com/css?family=PT+Sans:400,700&subset=latin,cyrillic"'

        then:
		    result.contains(expectedLink)
	}

	void testGoogleMapsInModule() {
        when:
            def template = '''<html>
                                <head>
                                  <r:require modules="google-maps"/>
                                  <r:layoutResources/>
                                </head>
                                <body>
                                  <h1>Hi</h1>
                                  <r:layoutResources/>
                                </body>
                              </html>'''
            def result = applyTemplate(template, [:])
            def expectedScript = '<script src="http://maps.googleapis.com/maps/api/js?libraries=places&sensor=false"'

        then:
		    result.contains(expectedScript)
	}

    void testDispositionsOfTransitiveDependencies() {
        when:
            String template = '''
                <r:require modules="GPRESOURCES-207_module_A"/>

                <r:layoutResources disposition="disposition_A"/>
                <r:layoutResources disposition="disposition_B"/>
                <r:layoutResources disposition="disposition_C"/>
                <r:layoutResources disposition="disposition_D"/>
            '''
            String result = applyTemplate(template)

        then:
            'direct dependency "GPRESOURCES-207_module_A" - resource for disposition C' == result.contains("/static/_bundle-bundle_GPRESOURCES-207_module_B_disposition_A.js")
            'direct dependency "GPRESOURCES-207_module_A" - resource for disposition D' == result.contains("/static/_bundle-bundle_GPRESOURCES-207_module_A_disposition_D.js")
            'transitive dependency "GPRESOURCES-207_module_B" - resource for disposition C' == result.contains("/static/_bundle-bundle_GPRESOURCES-207_module_B_disposition_C.js")
            // disposition B -- only via transitive dependency
            'transitive dependency "GPRESOURCES-207_module_B" - resource for disposition B' == result.contains("/static/_bundle-bundle_GPRESOURCES-207_module_B_disposition_B.js")
            // disposition A -- only via transitive dependency
            'transitive dependency "GPRESOURCES-207_module_B" - resource for disposition A' == result.contains("/static/_bundle-bundle_GPRESOURCES-207_module_B_disposition_A.js")
    }

    void testDispositionsOfTransitiveDependenciesWithStashedResource() {
        when:
            String template = '''
                <r:require modules="GPRESOURCES-207_module_A"/>
                <r:script disposition="disposition_B">
                /*stashed*/
                </r:script>

                <r:layoutResources disposition="disposition_A"/>
                <r:layoutResources disposition="disposition_B"/>
                <r:layoutResources disposition="disposition_C"/>
                <r:layoutResources disposition="disposition_D"/>
            '''
            String result = applyTemplate(template)

            new File('/tmp/testout.txt').text=result

        then:
            'direct dependency "GPRESOURCES-207_module_A" - resource for disposition C' == result.contains("/static/_bundle-bundle_GPRESOURCES-207_module_A_disposition_C.js")
            'direct dependency "GPRESOURCES-207_module_A" - resource for disposition D' == result.contains("/static/_bundle-bundle_GPRESOURCES-207_module_A_disposition_D.js")
            'transitive dependency "GPRESOURCES-207_module_B" - resource for disposition C' == result.contains("/static/_bundle-bundle_GPRESOURCES-207_module_B_disposition_C.js")
            'stashed script - disposition B' == result.contains("/*stashed*/")
            // disposition B -- via transitive dependency and stashed script
            'transitive dependency "GPRESOURCES-207_module_B" - resource for disposition B' == result.contains("/static/_bundle-bundle_GPRESOURCES-207_module_B_disposition_B.js")
            // disposition A -- only via transitive dependency
            'transitive dependency "GPRESOURCES-207_module_B" - resource for disposition A' == result.contains("/static/_bundle-bundle_GPRESOURCES-207_module_B_disposition_A.js")
    }


    void testDuplicateIncludes() {
        when:
            String template = '''
                <r:require modules="GPRESOURCES-210_module_A"/>
                <r:layoutResources disposition="duplicate_includes_check"/>
            '''

            String result = applyTemplate(template)

        then:
            1 == result.count("/static/_bundle-bundle_GPRESOURCES-210_module_A_duplicate_includes_check.js")
    }

    void testStyleTagWithDefaultDisposition() {
        when:
            String template = '''
                <r:style>
                /* stashed styles */
                </r:style>

                <r:layoutResources disposition="head"/>
            '''

            String result = applyTemplate(template)

        then:
            result ==~ /\s*<style type="text\/css">\s*\/\* stashed styles \*\/\s*<\/style>\s*/
    }

    void testStyleTagWithCustomDisposition() {
        when:
            String template = '''
                <r:style disposition="custom_disposition">
                /* stashed styles */
                </r:style>

                <r:layoutResources disposition="custom_disposition"/>
            '''

            String result = applyTemplate(template)

        then:
            result ==~ /\s*<style type="text\/css">\s*\/\* stashed styles \*\/\s*<\/style>\s*/
    }

    void testStashOfTypeScript() {
        when:
            String template = '''
                <r:stash type="script" disposition="script_stash_disposition">script stash</r:stash>
                <r:layoutResources disposition="script_stash_disposition"/>
            '''

            String result = applyTemplate(template)

        then:
            "<script type=\"text/javascript\">script stash</script>" == result.trim()
    }

    void testStashOfTypeScriptWithMultipleEntries() {
        when:
            String template = '''
                <r:stash type="script" disposition="script_stash_disposition">script stash1;</r:stash>
                <r:stash type="script" disposition="script_stash_disposition">script stash2;</r:stash>
                <r:layoutResources disposition="script_stash_disposition"/>
            '''

            String result = applyTemplate(template)

            String expected = "<script type=\"text/javascript\">script stash1;</script><script type=\"text/javascript\">script stash2;</script>"
        then:
            expected == result.trim()
    }

    void testStashOfTypeStyle() {
        when:
            String template = '''
                <r:stash type="style" disposition="style_stash_disposition">style stash</r:stash>
                <r:layoutResources disposition="style_stash_disposition"/>
            '''

            String result = applyTemplate(template)

        then:
            "<style type=\"text/css\">style stash</style>" == result.trim()
    }

    void testStashOfTypeStyleWithMultipleEntries() {
        when:
            String template = '''
                <r:stash type="style" disposition="style_stash_disposition">style stash1;</r:stash>
                <r:stash type="style" disposition="style_stash_disposition">style stash2;</r:stash>
                <r:layoutResources disposition="style_stash_disposition"/>
            '''

            String result = applyTemplate(template)

        then:
            "<style type=\"text/css\">style stash1;style stash2;</style>" == result.trim()
    }

    void testStashOfACustomType() {
        when:
            String type = "custom-stash"
            StashManager.STASH_WRITERS[type] = new FakeStashWriter()
            String template = """
                <r:stash type="${type}" disposition="${type}_stash_disposition">${type} stash</r:stash>
                <r:layoutResources disposition="${type}_stash_disposition"/>
            """

            String result = applyTemplate(template)
            // cleanup
            StashManager.STASH_WRITERS.remove(type)

        then:
            "<ul><li>custom-stash stash</li></ul>" == result.trim()
    }

    void testStashOfACustomTypeWithMultipleEntries() {
        when:
            String type = "custom-stash"
            StashManager.STASH_WRITERS[type] = new FakeStashWriter()
            String template = """
                <r:stash type="${type}" disposition="${type}_stash_disposition">${type} stash1;</r:stash>
                <r:stash type="${type}" disposition="${type}_stash_disposition">${type} stash2;</r:stash>
                <r:layoutResources disposition="${type}_stash_disposition"/>
            """

            String result = applyTemplate(template)
            // cleanup
            StashManager.STASH_WRITERS.remove(type)

        then:
            "<ul><li>custom-stash stash1;</li><li>custom-stash stash2;</li></ul>" == result.trim()
    }
}

class FakeStashWriter implements StashWriter {
    @Override
    void write(Writer out, List<String> stash) throws IOException {
        out << "<ul>"
        for (final String fragment in stash) {
            out << "<li>" << fragment << "</li>"
        }
        out << "</ul>"
    }
}
