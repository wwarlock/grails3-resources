package org.grails.plugin.resource

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin

import org.grails.plugin.resource.module.*
import org.junit.Before
import spock.lang.Specification

@TestMixin(GrailsUnitTestMixin)
class ResourceModulesBuilderSpec extends Specification{
    def svc

    @Before
    void setupTest() {
        svc = new Expando()
        svc.getDefaultSettingsForURI = { uri, type ->
            [:]
        }
    }

    void testModuleOverrides() {
        when:
            def modules = []
            def bld = new ModulesBuilder(modules)

            bld.'jquery' {
            }

            bld.'horn-smutils' {
                dependsOn(['jquery'])
            }

            bld.horn {
                defaultBundle false
                dependsOn(['horn-smutils', 'jquery'])
            }

            // knock out the smutils dep and replace
            bld.'smutils' {
                dependsOn(['jquery'])
            }

            bld.overrides {
                horn {
                    defaultBundle true
                    dependsOn(['smutils', 'jquery'])
                }
            }

        then:
            4 == modules.size()
            1 == bld._moduleOverrides.size()
            'horn' == bld._moduleOverrides[0].name
            true == bld._moduleOverrides[0].defaultBundle
            ['smutils', 'jquery'] == bld._moduleOverrides[0].dependencies
    }

    void testModuleDependsOnSyntaxes() {
        when:
            def modules = []
            def bld = new ModulesBuilder(modules)

            bld.'moduleA' {
                dependsOn(['jquery', 'jquery-ui'])
            }

            bld.'moduleB' {
                dependsOn 'jquery, jquery-ui'
            }

            bld.'moduleC' {
                dependsOn(['jquery', 'jquery-ui'] as String[])
            }

            shouldFail {
                bld.'moduleD' {
                    // This is bad groovy syntaxt, parens are needed, translates to getProperty('dependsOn')
                    dependsOn ['jquery', 'jquery-ui']
                }
            }

        then:
            3 == modules.size()
            ['jquery', 'jquery-ui'] == modules.find({it.name == 'moduleA'}).dependencies
            ['jquery', 'jquery-ui'] == modules.find({it.name == 'moduleB'}).dependencies
            ['jquery', 'jquery-ui'] == modules.find({it.name == 'moduleC'}).dependencies
    }

    void testDefaultBundleFalse() {
        when:
            def modules = []
            def bld = new ModulesBuilder(modules)

            bld.testModule {
                defaultBundle false
                resource url:'simile/simile.css'
                resource url:'simile/simile.js'
            }

        then:
            1 == modules.size()
            'testModule' == modules[0].name
            false == modules[0].defaultBundle
    }

    void testDefaultBundling() {
        when:
            def modules = []
            def bld = new ModulesBuilder(modules)

            bld.testModule {
                resource url:'simile/simile.css'
                resource url:'simile/simile.js'
            }

        then:
            1 == modules.size()
            'testModule' == modules[0].name
            null == modules[0].defaultBundle
    }

    void testDefaultBundleWithName() {
        when:
            def modules = []
            def bld = new ModulesBuilder(modules)

            bld.testModule {
                defaultBundle "frank-and-beans"
                resource url:'simile/simile.css'
                resource url:'simile/simile.js'
            }

        then:
            1 == modules.size()
            'testModule' == modules[0].name
            'frank-and-beans' == modules[0].defaultBundle
    }
}
