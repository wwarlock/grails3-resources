package org.grails.plugin.resources.artefacts;

import grails.core.ArtefactHandlerAdapter;
import grails.core.GrailsClass;

/**
 * @author Luke Daley (ld@ldaley.com)
 */
abstract public class AbstractResourcesArtefactHandler extends ArtefactHandlerAdapter {
    
    public AbstractResourcesArtefactHandler(String type, Class<? extends GrailsClass> grailsClassType, Class<?> grailsClassImpl, String artefactSuffix) {
        super(type, grailsClassType, grailsClassImpl, artefactSuffix, true);
    }
    
    @Override
    public String getPluginName() {
        return "resources";
    }
}