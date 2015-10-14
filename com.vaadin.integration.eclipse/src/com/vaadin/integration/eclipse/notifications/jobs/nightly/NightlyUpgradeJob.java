package com.vaadin.integration.eclipse.notifications.jobs.nightly;

import java.text.MessageFormat;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;

import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.ProjectDependencyManager;
import com.vaadin.integration.eclipse.util.ProjectUtil;
import com.vaadin.integration.eclipse.util.data.DownloadableVaadinVersion;
import com.vaadin.integration.eclipse.util.data.LocalVaadinVersion;
import com.vaadin.integration.eclipse.util.files.LocalFileManager;
import com.vaadin.integration.eclipse.util.network.DownloadManager;

/**
 * Background job that upgrades nightly builds in projects.
 */
public final class NightlyUpgradeJob extends Job {

    private final Map<IProject, DownloadableVaadinVersion> upgrades;

    public NightlyUpgradeJob(
            Map<IProject, DownloadableVaadinVersion> upgrades) {
        super(Messages.Notifications_UpgradeJobName);
        setUser(false);
        this.upgrades = upgrades;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        monitor.beginTask(Messages.Notifications_UpgradeTaskName,
                upgrades.size() * 6);

        try {
            if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            for (IProject project : upgrades.keySet()) {
                DownloadableVaadinVersion version = upgrades.get(project);

                // show project and new version in progress monitor
                monitor.subTask(MessageFormat.format(
                        Messages.Notifications_UpgradeSubTaskName,
                        project.getName(), version.getVersionNumber()));

                try {
                    upgradeProject(project, version, monitor);

                } catch (CoreException e) {
                    // TODO report also with a dialog?
                    ErrorUtil.handleBackgroundException(
                            "Failed to upgrade project " + project.getName() //$NON-NLS-1$
                                    + " to Vaadin " //$NON-NLS-1$
                                    + version.getVersionNumber(),
                            e);
                }
            }

            return Status.OK_STATUS;
        } finally {
            monitor.done();
        }
    }

    /**
     * Explicitly upgrade the Vaadin version in a project to the given version.
     * The version is downloaded if necessary.
     * 
     * @param project
     * @param newVersion
     * @param monitor
     * @return LocalVaadinVersion the version to which the project was upgraded
     * @throws CoreException
     */
    private static LocalVaadinVersion upgradeProject(IProject project,
            DownloadableVaadinVersion newVersion, IProgressMonitor monitor)
                    throws CoreException {
        // download version (if not already downloaded)
        DownloadManager.downloadVaadin(newVersion.getVersionNumber(),
                new SubProgressMonitor(monitor, 3));

        // upgrade
        ProjectUtil.ensureVaadinFacetAndNature(project);
        LocalVaadinVersion localVersion = LocalFileManager
                .getLocalVaadinVersion(newVersion.getVersionNumber());
        ProjectDependencyManager.updateVaadinLibraries(project, localVersion,
                new SubProgressMonitor(monitor, 3));
        return localVersion;
    }
}