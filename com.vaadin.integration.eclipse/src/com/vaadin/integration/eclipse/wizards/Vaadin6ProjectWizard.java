package com.vaadin.integration.eclipse.wizards;

import org.eclipse.wst.common.project.facet.core.IFacetedProjectTemplate;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

import com.vaadin.integration.eclipse.VaadinFacetUtils;

public class Vaadin6ProjectWizard extends VaadinProjectWizard {

    @Override
    protected String getProjectTypeTitle() {
        return "New Vaadin 6 Project (deprecated)";
    }

    @Override
    protected IFacetedProjectTemplate getTemplate() {
        return ProjectFacetsManager.getTemplate("template.vaadin6"); //$NON-NLS-1$
    }

    @Override
    protected String getDefaultPreset() {
        return VaadinFacetUtils.VAADIN6_PROJECT_DEFAULT_PRESET_ID;
    }

}
