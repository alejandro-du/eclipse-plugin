package com.vaadin.integration.eclipse.wizards;

import org.eclipse.wst.common.project.facet.core.IFacetedProjectTemplate;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

import com.vaadin.integration.eclipse.VaadinFacetUtils;

public class Vaadin7ProjectWizard extends VaadinProjectWizard {

    @Override
    protected String getProjectTypeTitle() {
        return "New Vaadin 7 Ivy Project (deprecated)";
    }

    @Override
    protected IFacetedProjectTemplate getTemplate() {
        return ProjectFacetsManager.getTemplate("template.vaadin7"); //$NON-NLS-1$
    }

    @Override
    protected String getDefaultPreset() {
        return VaadinFacetUtils.VAADIN7_PROJECT_DEFAULT_PRESET_ID;
    }

}
