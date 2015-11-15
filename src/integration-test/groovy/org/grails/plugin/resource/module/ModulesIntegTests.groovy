package org.grails.plugin.resource.module

import grails.test.mixin.integration.Integration
import grails.test.mixin.integration.IntegrationTestMixin
import grails.test.mixin.TestMixin
import grails.test.mixin.web.GroovyPageUnitTestMixin

/**
 * Integration tests of constructing modules. 
 * 
 * @author peter
 */
@Integration
@TestMixin(GroovyPageUnitTestMixin)
class ModulesIntegTests {
	
	def grailsResourceProcessor
	def grailsApplication
	
	protected makeMockResource(uri) {
		[
			uri:uri,
			disposition:'head',
			exists: { -> true }
		]
	}

	void testGrailsApplicationAccessInClosure() {

		def template = '''<html>
							<head>
							  <r:require modules="testAppAccess"/>
							  <r:layoutResources/>
							</head>
							<body>
							  <h1>Hi</h1>
							</body>
						  </html>'''
		def result = applyTemplate(template, [:])
		
		assertTrue result.contains("<!--${grailsApplication.ENV_DEVELOPMENT}-->")
	}
	
}