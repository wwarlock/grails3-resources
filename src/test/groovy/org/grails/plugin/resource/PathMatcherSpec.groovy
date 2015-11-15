package org.grails.plugin.resource

import org.springframework.util.AntPathMatcher
import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import spock.lang.Specification

@TestMixin(GrailsUnitTestMixin)
class PathMatcherSpec extends Specification {
    static final PATH_MATCHER = new AntPathMatcher()

    void testDeepMatching() {
        expect:
            PATH_MATCHER.match('**/.svn', 'web-app/images/.svn')
            !PATH_MATCHER.match('**/.svn', 'web-app/images/.svn/test.jpg')
            PATH_MATCHER.match('**/.svn/**/*.jpg', 'web-app/images/.svn/test.jpg')
            PATH_MATCHER.match('**/.svn/**/*.jpg', 'web-app/images/.svn/images/logos/test.jpg')
            !PATH_MATCHER.match('**/.svn/**/*.jpg', 'web-app/images/.svn/images/logos/test.png')
            PATH_MATCHER.match('**/.svn/**/*.*', 'web-app/images/.svn/images/logos/test.png')
            PATH_MATCHER.match('**/.svn/**/*.*', 'web-app/images/.svn/css/test.css')
    }
}
