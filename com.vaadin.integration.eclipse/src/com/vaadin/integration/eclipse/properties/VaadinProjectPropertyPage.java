package com.vaadin.integration.eclipse.properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.PropertyPage;

import com.vaadin.integration.eclipse.maven.MavenUtil;

/**
 * Property page grouping Vaadin related project properties.
 *
 * Vaadin version selection is here, future subpages may contain more settings.
 */
public class VaadinProjectPropertyPage extends PropertyPage {

    private IVaadinPropertyPage ivyPropertyPage = new VaadinIvyProjectPropertyPage();
    private IVaadinPropertyPage mavenPropertyPage = new VaadinMavenProjectPropertyPage();

    private IVaadinPropertyPage topPage = ivyPropertyPage;

    private Composite root;
    private StackLayout stackLayout;
    private IProject project;

    public VaadinProjectPropertyPage() {
    }

    @Override
    protected void performDefaults() {
        getTopPage().performDefaults();
    }

    @Override
    public boolean performOk() {
        return getTopPage().performOk();
    }

    /**
     * @see PreferencePage#createContents(Composite)
     */
    @Override
    protected Control createContents(Composite parent) {
        root = new Composite(parent, SWT.NONE);
        stackLayout = new StackLayout();
        root.setLayout(stackLayout);

        // this also creates the contents and sets topPage as stackLayout is no
        // longer null
        setProject(project);

        return root;
    }

    @Override
    public void setElement(IAdaptable element) {
        super.setElement(element);

        IProject project = null;
        if (getElement() instanceof IJavaProject) {
            project = ((IJavaProject) getElement()).getProject();
        } else if (getElement() instanceof IProject) {
            project = (IProject) getElement();
        }

        // stackLayout can be null at this point
        setProject(project);
    }

    private void setProject(IProject project) {
        this.project = project;

        if (stackLayout != null) {
            if (MavenUtil.isMavenProject(project)) {
                mavenPropertyPage.setProject(project);
                if (mavenPropertyPage.getControl() == null) {
                    mavenPropertyPage.createContents(root);
                }
                setTopPage(mavenPropertyPage);
            } else {
                ivyPropertyPage.setProject(project);
                if (ivyPropertyPage.getControl() == null) {
                    ivyPropertyPage.createContents(root);
                }
                setTopPage(ivyPropertyPage);
            }
        }
    }

    @Override
    public void dispose() {
        super.dispose();

        ivyPropertyPage.dispose();
        mavenPropertyPage.dispose();
    }

    public IVaadinPropertyPage getTopPage() {
        return topPage;
    }

    public void setTopPage(IVaadinPropertyPage topPage) {
        this.topPage = topPage;
        stackLayout.topControl = topPage.getControl();
        root.layout();
    }
}