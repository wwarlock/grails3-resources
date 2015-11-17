package org.grails.plugin.resource

import grails.test.mixin.integration.Integration
import spock.lang.Specification

@Integration
class ResourceProcessorIntegSpec extends Specification {

    def grailsResourceProcessor

    protected makeMockResource(uri) {
        [
            uri:uri,
            disposition:'head',
            exists: { -> true }
        ]
    }

    void testGettingModulesInDependencyOrder() {
        when:
            def testModules = [
                a: [name:'a', resources: [ makeMockResource('a.css') ] ],
                b: [name:'b', dependsOn:['a'], resources: [ makeMockResource('b.css') ] ],
                c: [name:'c', dependsOn:['a', 'b'], resources: [ makeMockResource('a.css') ] ],
                d: [name:'d', dependsOn:['b'], resources: [ makeMockResource('a.css') ] ],
                e: [name:'e', dependsOn:['d'], resources: [ makeMockResource('a.css') ] ]
            ]

            def modsNeeded = [
                e: true,
                c: true
            ]

            grailsResourceProcessor.modulesByName.putAll(testModules)
            grailsResourceProcessor.updateDependencyOrder()

            def moduleNames = grailsResourceProcessor.getAllModuleNamesRequired(modsNeeded)
            println "Module names: ${moduleNames}"
            def moduleNameResults = grailsResourceProcessor.getModulesInDependencyOrder(moduleNames)
            println "Modules: ${moduleNameResults}"

        then:
            moduleNameResults.indexOf('a') < moduleNameResults.indexOf('b')
            moduleNameResults.indexOf('b') < moduleNameResults.indexOf('c')
            moduleNameResults.indexOf('b') < moduleNameResults.indexOf('d')
            moduleNameResults.indexOf('b') < moduleNameResults.indexOf('e')
            moduleNameResults.indexOf('d') < moduleNameResults.indexOf('e')
    }
}
