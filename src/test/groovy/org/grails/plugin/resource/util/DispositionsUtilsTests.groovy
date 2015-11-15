package org.grails.plugin.resource.util

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification


@TestMixin(GrailsUnitTestMixin)
class DispositionsUtilsTests extends Specification {

    void testAddingDispositionToRequest() {
        when:
            def request = [:]
        then:
            DispositionsUtils.getRequestDispositionsRemaining(request).empty

        when:
            DispositionsUtils.addDispositionToRequest(request, 'head', 'dummy')
        then:
            (['head'] as Set) == DispositionsUtils.getRequestDispositionsRemaining(request)

        when:
            // Let's just make sure its a set
            DispositionsUtils.addDispositionToRequest(request, 'head', 'dummy')
        then:
            (['head'] as Set) == DispositionsUtils.getRequestDispositionsRemaining(request)

        when:
            DispositionsUtils.addDispositionToRequest(request, 'defer', 'dummy')
        then:
            (['head', 'defer'] as Set) == DispositionsUtils.getRequestDispositionsRemaining(request)

        when:
            DispositionsUtils.addDispositionToRequest(request, 'image', 'dummy')
        then:
            (['head', 'image', 'defer'] as Set) == DispositionsUtils.getRequestDispositionsRemaining(request)
    }

    void testRemovingDispositionFromRequest() {
        when:
            def request = [(DispositionsUtils.REQ_ATTR_DISPOSITIONS_REMAINING): (['head', 'image', 'defer'] as Set)]
        then:
            (['head', 'image', 'defer'] as Set) == DispositionsUtils.getRequestDispositionsRemaining(request)

        when:
            DispositionsUtils.removeDispositionFromRequest(request, 'head')
        then:
            (['defer', 'image'] as Set) == DispositionsUtils.getRequestDispositionsRemaining(request)

        when:
            DispositionsUtils.removeDispositionFromRequest(request, 'defer')
        then:
            (['image'] as Set) == DispositionsUtils.getRequestDispositionsRemaining(request)
    }

}
