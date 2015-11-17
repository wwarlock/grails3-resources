package org.grails.plugin.resources.stash

import grails.test.mixin.integration.Integration
import org.springframework.mock.web.MockHttpServletRequest
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest

/**
 * Integration tests for {@link StashManager}.
 *
 * @author Patrick Jungermann
 */
@Integration
class StashManagerIntegrationSpec extends Specification {

    void shouldBeInitializedWithStashWriterForScripts() {
        expected:
            StashManager.STASH_WRITERS["script"] instanceof ScriptStashWriter
    }

    void shouldBeInitializedWithStashWriterForStyles() {
        expected:
            StashManager.STASH_WRITERS["style"] instanceof StyleStashWriter
    }

    void stashFragmentButNoRequest() {
        when:
            String type = "fragment-type"
            String disposition = "my-disposition"
            String fragment = "test fragment"

            StashManager.stashPageFragment(null, type, disposition, fragment)

        then:
            thrown NullPointerException
    }

    void stashScriptFragment() {
        when:
            HttpServletRequest request = new MockHttpServletRequest()
            String type = "script"
            String disposition = "my-disposition"
            String fragment = "test fragment"

            StashManager.stashPageFragment(request, type, disposition, fragment)

            List<String> stash = (List<String>) request["resources.plugin.page.fragments:script:my-disposition"]

        then:
            stash.contains(fragment)
    }

    void stashStyleFragment() {
        when:
            HttpServletRequest request = new MockHttpServletRequest()
            String type = "style"
            String disposition = "my-disposition"
            String fragment = "test fragment"

            StashManager.stashPageFragment(request, type, disposition, fragment)

            List<String> stash = (List<String>) request["resources.plugin.page.fragments:style:my-disposition"]

        then:
            stash.contains(fragment)
    }

    void stashCustomTypedFragment() {
        when:
            HttpServletRequest request = new MockHttpServletRequest()
            String type = "custom-type"
            String disposition = "my-disposition"
            String fragment = "test fragment"

            StashManager.stashPageFragment(request, type, disposition, fragment)

            List<String> stash = (List<String>) request["resources.plugin.page.fragments:custom-type:my-disposition"]

        then:
            stash.contains(fragment)
    }

    void unstashScriptFragments() {
        when:
            StringWriter writer = new StringWriter()
            HttpServletRequest request = new MockHttpServletRequest()
            String disposition = "my-disposition"

            request["resources.plugin.page.fragments:script:my-disposition"] = [
                    "script-fragment-1;",
                    "script-fragment-2;"
            ]

            StashManager.unstashPageFragments(writer, request, disposition)

            String script = "<script type=\"text/javascript\">script-fragment-1;</script><script type=\"text/javascript\">script-fragment-2;</script>"

        then:
            script == writer.toString()
    }

    void unstashStyleFragments() {
        when:
            StringWriter writer = new StringWriter()
            HttpServletRequest request = new MockHttpServletRequest()
            String disposition = "my-disposition"

            request["resources.plugin.page.fragments:style:my-disposition"] = [
                    "style-fragment-1;",
                    "style-fragment-2;"
            ]

            StashManager.unstashPageFragments(writer, request, disposition)

            String style = "<style type=\"text/css\">style-fragment-1;style-fragment-2;</style>"

        then:
            style == writer.toString()
    }

    void unstashCustomTypedFragments() {
        when:
            StringWriter writer = new StringWriter()
            HttpServletRequest request = new MockHttpServletRequest()
            String disposition = "my-disposition"

            StashManager.STASH_WRITERS["custom-type"] = new CustomTypeStashWriter()
            request["resources.plugin.page.fragments:custom-type:my-disposition"] = [
                    "script-fragment-1;",
                    "script-fragment-2;"
            ]

            StashManager.unstashPageFragments(writer, request, disposition)

            String expected = "<ul><li>script-fragment-1;</li><li>script-fragment-2;</li></ul>"

        then:
            expected == writer.toString()
    }

    void unstashFragmentsOfMultipleTypes() {
        when:
            StringWriter writer = new StringWriter()
            HttpServletRequest request = new MockHttpServletRequest()
            String disposition = "my-disposition"

            request["resources.plugin.page.fragments:script:my-disposition"] = [
                    "script-fragment-1;",
                    "script-fragment-2;"
            ]
            request["resources.plugin.page.fragments:style:my-disposition"] = [
                    "style-fragment-1;",
                    "style-fragment-2;"
            ]

            StashManager.unstashPageFragments(writer, request, disposition)

            String style = "<style type=\"text/css\">style-fragment-1;style-fragment-2;</style>"
            String script = "<script type=\"text/javascript\">script-fragment-1;</script><script type=\"text/javascript\">script-fragment-2;</script>"

        then:
            style + script == writer.toString()
    }

    void ignoreStashesWithoutRegisteredStashWriter() {
        when:
            StringWriter writer = new StringWriter()
            HttpServletRequest request = new MockHttpServletRequest()
            String disposition = "my-disposition"

            request["resources.plugin.page.fragments:script:my-disposition"] = [
                    "script-fragment-1;",
            ]
            request["resources.plugin.page.fragments:ignored:my-disposition"] = [
                    "script-fragment-1;",
            ]

            StashManager.unstashPageFragments(writer, request, disposition)

            String script = "<script type=\"text/javascript\">script-fragment-1;</script>"

        then:
            script == writer.toString()
    }

    void unstashScriptFragmentsButNoWriter() {
        when:
            HttpServletRequest request = new MockHttpServletRequest()
            String disposition = "my-disposition"

            request["resources.plugin.page.fragments:script:my-disposition"] = [
                    "script-fragment-1;",
                    "script-fragment-2;"
            ]

            StashManager.unstashPageFragments(null, request, disposition)
        then:
            thrown NullPointerException
    }

    void unstashScriptFragmentsButNoRequest() {
        when:
            StringWriter writer = new StringWriter()
            String disposition = "my-disposition"

            StashManager.unstashPageFragments(writer, null, disposition)
        then:
            thrown NullPointerException
    }



}

class CustomTypeStashWriter implements StashWriter {

    @Override
    void write(Writer out, List<String> stash) throws IOException {
        out << "<ul>"
        for (String fragment in stash) {
            out << "<li>"
            out << fragment
            out << "</li>"
        }
        out << "</ul>"
    }

}
