package org.grails.plugin.resource

import grails.test.*
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification

import javax.servlet.FilterChain

import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockHttpServletRequest

@TestMixin(GrailsUnitTestMixin)
class ProcessingFilterSpec extends Specification {

    void testResourceIsNotProcessedByBothFiltersIfHandledByFirst() {
        when:
            def filter = new ProcessingFilter()
            filter.adhoc = false
            filter.grailsResourceProcessor = [
                isDebugMode: { req -> false },
                processModernResource: { req, resp -> resp.committed = true }
            ]

            def rq = new MockHttpServletRequest()
            def rp = new MockHttpServletResponse()

            def fakeChain = [
                doFilter: { req, resp -> fail('Second filter instance was called') }
            ] as FilterChain

        then:
            filter.doFilter(rq, rp, fakeChain)
    }
}