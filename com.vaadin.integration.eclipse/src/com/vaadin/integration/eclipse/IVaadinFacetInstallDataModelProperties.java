package com.vaadin.integration.eclipse;

import org.eclipse.wst.common.frameworks.datamodel.IDataModelProperties;

/**
 * Property names in the facet data model.
 */
public interface IVaadinFacetInstallDataModelProperties extends
        IDataModelProperties {

    public static final String APPLICATION_NAME = "IVaadinFacetInstallDataModelProperties.APPLICATION_NAME"; //$NON-NLS-1$
    public static final String APPLICATION_PACKAGE = "IVaadinFacetInstallDataModelProperties.APPLICATION_PACKAGE"; //$NON-NLS-1$
    public static final String APPLICATION_CLASS = "IVaadinFacetInstallDataModelProperties.APPLICATION_CLASS"; //$NON-NLS-1$
    public static final String APPLICATION_THEME = "IVaadinFacetInstallDataModelProperties.APPLICATION_THEME"; //$NON-NLS-1$

    public static final String CREATE_ARTIFACTS = "IVaadinFacetInstallDataModelProperties.CREATE_ARTIFACTS"; //$NON-NLS-1$

    /**
     * Value is one of the PORTLET_VERSION_* constants.
     */
    public static final String PORTLET_VERSION = "IVaadinFacetInstallDataModelProperties.CREATE_PORTLET"; //$NON-NLS-1$

    public static final String PORTLET_VERSION_NONE = "No portlet";
    public static final String PORTLET_VERSION10 = "Portlet 1.0";
    public static final String PORTLET_VERSION20 = "Portlet 2.0";

    public static final String PORTLET_TITLE = "IVaadinFacetInstallDataModelProperties.PORTLET_TITLE"; //$NON-NLS-1$

    public static final String VAADIN_VERSION = "IVaadinFacetInstallDataModelProperties.VAADIN_VERSION"; //$NON-NLS-1$

    public static final String VAADIN_PROJECT_TYPE = "IVaadinFacetInstallDataModelProperties.VAADIN_PROJECT_TYPE"; //$NON-NLS-1$

    public static final String CREATE_TB_TEST = "IVaadinFacetInstallDataModelProperties.CREATE_TB_TEST"; //$NON-NLS-1$
}
