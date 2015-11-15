package org.grails.plugin.resource

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin

import org.grails.plugin.resource.mapper.ResourceMapper
import spock.lang.Specification

@TestMixin(GrailsUnitTestMixin)
class ResourceMapperSpec extends Specification {
    void testDefaultIncludesExcludes() {
        when:
            def artefact = new DummyMapper()
            artefact.defaultExcludes = ['**/*.jpg', '**/*.png']
            artefact.defaultIncludes = ['**/*.*']
            artefact.name = 'foobar'
            artefact.map = { res, config ->
            }

            def m = new ResourceMapper(artefact, [foobar:[:]])

            def testMeta = new ResourceMeta()
            testMeta.sourceUrl = '/images/test.png'
            testMeta.actualUrl = '/images/test.png'
            testMeta.contentType = "image/png"

        then:
            !m.invokeIfNotExcluded(testMeta)

        when:
            def testMetaB = new ResourceMeta()
            testMetaB.sourceUrl = '/images/test.jpg'
            testMetaB.actualUrl = '/images/test.jpg'
            testMetaB.contentType = "image/jpeg"

        then:
            !m.invokeIfNotExcluded(testMetaB)

        when:
            def testMeta2 = new ResourceMeta()
            testMeta2.sourceUrl = '/images/test.zip'
            testMeta2.actualUrl = '/images/test.zip'
            testMeta2.contentType = "application/zip"

        then:
            m.invokeIfNotExcluded(testMeta2)

    }

    void testResourceExclusionOfMapper() {
        when:
            def artefact = new DummyMapper()
            artefact.defaultIncludes = ['**/*.*']
            artefact.name = 'minify'
            artefact.map = { res, config ->
            }

            def m = new ResourceMapper(artefact, [minify:[:]])

            def artefact2 = new DummyMapper()
            artefact2.defaultIncludes = ['**/*.*']
            artefact2.name = 'other'
            artefact2.map = { res, config ->
            }

            def m2 = new ResourceMapper(artefact2, [other:[:]])

            def testMeta = new ResourceMeta()
            testMeta.sourceUrl = '/images/test.png'
            testMeta.actualUrl = '/images/test.png'
            testMeta.contentType = "image/png"
            testMeta.excludedMappers = ['minify'] as Set

        then:
            !m.invokeIfNotExcluded(testMeta)
            m2.invokeIfNotExcluded(testMeta)
    }

    void testResourceExclusionOfOperation() {
        when:
            def artefact = new DummyMapper()
              artefact.defaultIncludes = ['**/*.*']
              artefact.name = 'yuicssminifier'
              artefact.operation = 'minify'
              artefact.map = { res, config ->
              }

              def m = new ResourceMapper(artefact, [minify:[:]])

              def artefact2 = new DummyMapper()
              artefact2.defaultIncludes = ['**/*.*']
              artefact2.name = 'googlecssminifier'
              artefact2.operation = 'minify'
              artefact2.map = { res, config ->
              }

              def m2 = new ResourceMapper(artefact2, [other:[:]])

              def testMeta = new ResourceMeta()
              testMeta.sourceUrl = '/images/test.css'
              testMeta.actualUrl = '/images/test.css'
              testMeta.contentType = "text/css"
              testMeta.excludedMappers = ['minify'] as Set

        then:
          !m.invokeIfNotExcluded(testMeta)
          !m2.invokeIfNotExcluded(testMeta)

        when:
          testMeta.excludedMappers = null

        then:
          m.invokeIfNotExcluded(testMeta)
          m2.invokeIfNotExcluded(testMeta)
    }
}

class DummyMapper {
    def defaultExcludes
    def defaultIncludes
    def name
    def map
    def operation
}