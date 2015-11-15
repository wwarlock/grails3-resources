package org.grails.plugin.resources.stash

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.junit.Before
import org.junit.Test
import spock.lang.Shared
import spock.lang.Specification

/**
 * Unit tests for {@link ScriptStashWriter}.
 *
 * @author Patrick Jungermann
 */
@TestMixin(GrailsUnitTestMixin)
class ScriptStashWriterUnitSpec extends Specification {

    @Shared
    ScriptStashWriter writer

    @Before
    void setup() {
        writer = new ScriptStashWriter()
    }

    void writeButNoOutputTarget() {
        when:
            writer.write(null, ["fragment"])
        then:
            thrown NullPointerException
    }

    void writeButNoStash() {
        when:
            writer.write(new StringWriter(), null)
        then:
            thrown NullPointerException
    }

    @Test
    void writeEmptyStash() {
        when:
            StringWriter out = new StringWriter()
            writer.write(out, [])

        then:
            "" == out.toString()
    }

    @Test
    void writeStashWithOneFragment() {
        when:
            StringWriter out = new StringWriter()
            writer.write(out, ["fragment;"])

        then:
            String expected = "<script type=\"text/javascript\">fragment;</script>"
            expected == out.toString()
    }

    @Test
    void writeStashWithMultipleFragments() {
        when:
            StringWriter out = new StringWriter()
            writer.write(out, ["fragment1;", "fragment2;"])

        then:
            String expected = "<script type=\"text/javascript\">fragment1;</script><script type=\"text/javascript\">fragment2;</script>"
            expected == out.toString()
    }

}
