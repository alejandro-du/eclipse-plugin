package com.vaadin.integration.eclipse;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;

import com.vaadin.integration.eclipse.util.ErrorUtil;

public class NewProjectListener implements IResourceChangeListener {

    public void resourceChanged(IResourceChangeEvent event) {
        try {
            event.getDelta().accept(new NewProjectVisitor());
        } catch (CoreException e) {
            ErrorUtil.handleBackgroundException(e);
        }
    }

    private class NewProjectVisitor implements IResourceDeltaVisitor {

        public boolean visit(IResourceDelta delta) {
            final IResource res = delta.getResource();
            // Defer the check for AddonStylesBuilder in order to detect
            // reliably whether an imported project is a Vaadin project.
            if (delta.getKind() == IResourceDelta.ADDED
                    && res instanceof IProject) {
                CheckAddonStylesBuilderJob job = new CheckAddonStylesBuilderJob(
                        "Check AddonStylesBuilder", (IProject) res);
                job.setUser(false);
                job.schedule(30 * 1000);
                return false;
            }
            // It is only necessary to visit the children of the workspace root
            // since projects are its direct descendants
            return res instanceof IWorkspaceRoot;
        }
    }
}