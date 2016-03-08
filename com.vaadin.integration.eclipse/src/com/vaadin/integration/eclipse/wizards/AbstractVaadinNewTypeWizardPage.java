package com.vaadin.integration.eclipse.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.wizards.NewContainerWizardPage;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.ProjectUtil;

public abstract class AbstractVaadinNewTypeWizardPage extends NewTypeWizardPage {

    protected IProject project;

    public AbstractVaadinNewTypeWizardPage(String pageName, IProject project) {
        super(true, pageName);

        setProject(project);
    }

    /**
     * Set the project for the wizard page, updating the wizard internal fields
     * by calling setPackageFragmentRoot.
     * 
     * @param project
     */
    public void setProject(IProject project) {
        IJavaProject jp = JavaCore.create(project);
        // do as other wizards do: allow showing page even if no project
        // exists
        if (jp != null && ProjectUtil.isVaadin7(project)) {
            // this will also call setProjectInternal(IProject)
            try {
                IPackageFragmentRoot[] roots = jp.getPackageFragmentRoots();
                for (IPackageFragmentRoot root : roots) {
                    if (!root.isArchive()
                            && root.getKind() == IPackageFragmentRoot.K_SOURCE) {
                        setPackageFragmentRoot(root, true);
                        break;
                    }
                }
            } catch (JavaModelException e1) {
                ErrorUtil.handleBackgroundException(IStatus.WARNING,
                        "Failed to select the project for the wizard", e1);
            }
        }
    }

    /**
     * This is only called from setPackageFragmentRoot, and should be overridden
     * to update fields based on project change.
     * 
     * The overriding method should first call super.setProjectInternal().
     */
    protected void setProjectInternal(IProject project) {
        this.project = project;

        if (project == null || !ProjectUtil.isVaadin7(project)) {
            setPackageFragment(null, false);
            setTypeName("", false);
            return;
        }
    }

    public IProject getProject() {
        return project;

    }

    @Override
    public void setPackageFragmentRoot(IPackageFragmentRoot root,
            boolean canBeModified) {
        super.setPackageFragmentRoot(root, canBeModified);
        // call setProject to update application list and other fields when the
        // project/source location is changed
        IProject newProject = root.getJavaProject().getProject();
        setProjectInternal(newProject);

        // make sure the status is updated after setProject()
        handleFieldChanged(NewContainerWizardPage.CONTAINER);
    }

    /*
     * @see WizardPage#becomesVisible
     * 
     * @see NewElementWizardPage#setVisible
     */
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        // make sure everything is updated based on the selected project
        setProject(project);
    }

    /*
     * @see NewContainerWizardPage#handleFieldChanged
     */
    @Override
    protected void handleFieldChanged(String fieldName) {
        super.handleFieldChanged(fieldName);

        // highest priority for the Vaadin specific errors
        // all used component status
        IStatus[] status = getStatus();

        // the most severe status will be displayed and the OK button
        // enabled/disabled.
        updateStatus(status);
    }

    protected IStatus[] getStatus() {
        IStatus[] status;
        if (project == null || !ProjectUtil.isVaadin7(project)) {
            status = new Status[] { new Status(IStatus.ERROR,
                    VaadinPlugin.PLUGIN_ID, "No suitable project found.") };
        } else {
            status = new IStatus[] { fContainerStatus, fPackageStatus,
                    fTypeNameStatus };
        }
        return status;
    }

}
