package com.vaadin.integration.eclipse.background;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.PlatformUI;

import com.vaadin.integration.eclipse.VaadinFacetUtils;
import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.preferences.PreferenceConstants;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.PreferenceUtil;
import com.vaadin.integration.eclipse.util.ProjectUtil;
import com.vaadin.integration.eclipse.util.VersionUtil;
import com.vaadin.integration.eclipse.util.data.DownloadableVaadinVersion;
import com.vaadin.integration.eclipse.util.data.MavenVaadinVersion;
import com.vaadin.integration.eclipse.util.network.DownloadManager;
import com.vaadin.integration.eclipse.util.network.MavenVersionManager;

/**
 * User-visible job that checks for new vaadin versions then re-schedules a new
 * check.
 */
public final class CheckVaadinVersionsJob extends Job {
    public CheckVaadinVersionsJob(String name) {
        super(name);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {

        if (VaadinPlugin
                .getInstance()
                .getPreferenceStore()
                .getBoolean(
                        PreferenceConstants.DISABLE_ALL_UPDATE_NOTIFICATIONS)) {
            // if the preference is true, update queries and usage reporting is
            // disabled

            return Status.OK_STATUS;

        }
        try {
            monitor.beginTask("Checking which Vaadin versions are used", 4);

            if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            // map from project with "use latest nightly" to the current
            // Vaadin version number string in the project
            Map<IProject, String> vaadinProjects = getVaadinProjects();
            monitor.worked(1);

            if (vaadinProjects.isEmpty()) {
                return Status.OK_STATUS;
            } else if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            monitor.worked(1);

            // create a task to phone home with Vaadin versions in use
            ReportVaadinUsageStatistics phoneHome = new ReportVaadinUsageStatistics(
                    "Report usage statistics", vaadinProjects);
            phoneHome.setUser(false);
            phoneHome.schedule();

            monitor.worked(2);
        } finally {
            monitor.done();
        }

        return Status.OK_STATUS;

    }

    /**
     * Returns the open projects in the workspace which use Vaadin
     * 
     * @return
     */
    private Map<IProject, String> getVaadinProjects() {
        Map<IProject, String> projectsWithVaadin = new HashMap<IProject, String>();
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IProject[] projects = workspaceRoot.getProjects();
        for (IProject project : projects) {
            try {
                // Only search in open java projects which can have a Vaadin
                // library version
                if (project.isOpen() && project.hasNature(JavaCore.NATURE_ID)) {
                    String versionNumber = ProjectUtil
                            .getVaadinLibraryVersion(project, true);
                    if (null != versionNumber) {
                        projectsWithVaadin.put(project, versionNumber);
                    }
                }
            } catch (CoreException e) {
                ErrorUtil.handleBackgroundException(
                        IStatus.WARNING,
                        "Could not check Vaadin version in project "
                                + project.getName(), e);
            }
        }
        return projectsWithVaadin;
    }

}