package com.vaadin.integration.eclipse.properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * An interface for project type specific property pages. Most of the methods
 * here correspond to methods in {@link PropertyPage}.
 */
public interface IVaadinPropertyPage {

    public void performDefaults();

    public boolean performOk();

    public Control createContents(Composite parent);

    public void dispose();

    public Control getControl();

    /**
     * Set the project for which to display properties.
     * 
     * @param project
     *            the project to configure, can be null
     */
    public void setProject(IProject project);

    /**
     * Return the project being modified (if any).
     * 
     * @return project, can be null
     */
    public IProject getProject();

}