package org.grails.plugin.resources.artefacts;

import org.grails.core.AbstractGrailsClass;

/**
 * @author Luke Daley (ld@ldaley.com)
 */
public class DefaultResourcesClass extends AbstractGrailsClass implements ResourcesClass {
    public DefaultResourcesClass(Class clazz) {
        super(clazz, ResourcesArtefactHandler.SUFFIX);
    }
}