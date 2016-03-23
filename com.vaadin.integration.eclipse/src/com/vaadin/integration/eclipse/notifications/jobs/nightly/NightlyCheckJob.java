package com.vaadin.integration.eclipse.notifications.jobs.nightly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.notifications.Consumer;
import com.vaadin.integration.eclipse.notifications.ProjectsUpgradeInfo;
import com.vaadin.integration.eclipse.notifications.Utils;
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
abstract class NightlyCheckJob extends Job {

    private static final Logger LOG = Logger
            .getLogger(NightlyCheckJob.class.getName());

    NightlyCheckJob() {
        super(Messages.Notifications_NightlyCheckJobName);

        setUser(false);
        // avoid concurrent checks and upgrades
        setRule(NightlyCheckRule.getInstance());
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        monitor.beginTask(Messages.Notifications_NightlyCheckJobName, 4);
        try {
            if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            return checkProjects(monitor);
        } catch (CoreException e) {
            ErrorUtil.handleBackgroundException(
                    "Failed to update Vaadin nightly build list", e); //$NON-NLS-1$
            return new Status(IStatus.WARNING, VaadinPlugin.PLUGIN_ID, 1,
                    "Failed to update Vaadin nightly build list", e); //$NON-NLS-1$
        } finally {
            monitor.done();
        }
    }

    protected abstract Consumer<ProjectsUpgradeInfo> getConsumer();

    private IStatus checkProjects(IProgressMonitor monitor)
            throws CoreException {
        // map from project with "use latest nightly" to the current
        // Vaadin version number string in the project
        Map<IProject, String> nightlyProjects = getProjectsUsingLatestNightly();
        List<IProject> vaadin7Projects = getVaadin7Projects();

        LOG.info(
                "Projects with nightly versions : " + nightlyProjects.keySet()); //$NON-NLS-1$
        LOG.info("All vaadin 7 projects : " + vaadin7Projects); //$NON-NLS-1$

        monitor.worked(1);

        if (nightlyProjects.isEmpty() && vaadin7Projects.isEmpty()) {
            return Status.OK_STATUS;
        } else if (monitor.isCanceled()) {
            return Status.CANCEL_STATUS;
        }

        // update version list
        List<DownloadableVaadinVersion> availableNightlies = DownloadManager
                .getAvailableNightlyVersions();

        LOG.fine("Available nightlies : " + availableNightlies); //$NON-NLS-1$

        monitor.worked(1);

        final Map<IProject, DownloadableVaadinVersion> possibleUpgrades = new HashMap<IProject, DownloadableVaadinVersion>();

        for (IProject project : nightlyProjects.keySet()) {
            String currentVersion = nightlyProjects.get(project);
            DownloadableVaadinVersion latestNightly = Utils
                    .getNightlyToUpgradeTo(currentVersion, availableNightlies);

            if (null != latestNightly && !latestNightly.getVersionNumber()
                    .equals(currentVersion)) {
                possibleUpgrades.put(project, latestNightly);
            }
        }

        LOG.info("Nightly projects to upgrade : " + possibleUpgrades.keySet()); //$NON-NLS-1$

        final Map<IProject, List<MavenVaadinVersion>> vaadin7Upgrades = getVaadinUpgrades(
                vaadin7Projects);

        LOG.info("Vaadin 7 projects to upgrade : " + vaadin7Upgrades.keySet()); //$NON-NLS-1$

        monitor.worked(1);

        if (possibleUpgrades.isEmpty() && vaadin7Upgrades.isEmpty()) {
            return Status.OK_STATUS;
        } else if (monitor.isCanceled()) {
            return Status.CANCEL_STATUS;
        }

        // create new task to upgrade Vaadin nightly builds in projects
        NightlyUpgradeJob upgradeJob = new NightlyUpgradeJob(possibleUpgrades);
        // avoid concurrent checks and upgrades, "lock" the workspace
        upgradeJob.setRule(MultiRule.combine(NightlyUpgradeRule.getInstance(),
                ResourcesPlugin.getWorkspace().getRoot()));
        upgradeJob.schedule();

        monitor.worked(1);

        getConsumer().accept(
                new ProjectsUpgradeInfo(possibleUpgrades, vaadin7Upgrades));

        return Status.OK_STATUS;
    }

    /**
     * Returns the open projects in the workspace for which the
     * "Use latest nightly" option is selected.
     * 
     * @return
     */
    private Map<IProject, String> getProjectsUsingLatestNightly() {
        Map<IProject, String> projectsWithNightly = new HashMap<IProject, String>();
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IProject[] projects = workspaceRoot.getProjects();
        for (IProject project : projects) {
            try {
                if (!project.isOpen() || !project.hasNature(JavaCore.NATURE_ID)) {
                    continue;
                }
                // add if "use latest nightly" is set
                PreferenceUtil preferences = PreferenceUtil.get(project);
                if (preferences.isUsingLatestNightly()) {
                    String versionNumber = ProjectUtil
                            .getVaadinLibraryVersion(project, true);
                    if (null != versionNumber) {
                        projectsWithNightly.put(project, versionNumber);
                    }
                }
            } catch (CoreException e) {
                ErrorUtil.handleBackgroundException(IStatus.WARNING,
                        "Could not check Vaadin version in project " //$NON-NLS-1$
                                + project.getName(),
                        e);
            }
        }
        return projectsWithNightly;
    }

    private List<IProject> getVaadin7Projects() {
        List<IProject> vaadin7Projects = new ArrayList<IProject>();
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IProject[] projects = workspaceRoot.getProjects();
        for (IProject project : projects) {
            try {
                if (!project.isOpen() || !project.hasNature(JavaCore.NATURE_ID)) {
                    continue;
                }
                if (ProjectUtil.isVaadin7(project)) {
                    vaadin7Projects.add(project);
                }
            } catch (CoreException e) {
                ErrorUtil.handleBackgroundException(IStatus.WARNING,
                        "Could not check Vaadin version in project " //$NON-NLS-1$
                        + project.getName(),
                        e);
            }
        }
        return vaadin7Projects;
    }

    private Map<IProject, List<MavenVaadinVersion>> getVaadinUpgrades(
            List<IProject> vaadinProjects) {
        Map<IProject, List<MavenVaadinVersion>> availableUpgrades = new HashMap<IProject, List<MavenVaadinVersion>>();
        List<MavenVaadinVersion> availableVersions = new ArrayList<MavenVaadinVersion>();
        try {
            availableVersions = MavenVersionManager.getAvailableVersions(true);
        } catch (CoreException e) {
            // Could not load the list of upgrades, handle as if none were
            // available.
            return availableUpgrades;
        }
        for (IProject project : vaadinProjects) {
            try {
                if (!PreferenceUtil.get(project)
                        .isUpdateNotificationEnabled()) {
                    continue;
                }
                String currentVersion = ProjectUtil
                        .getVaadinLibraryVersion(project, true);
                if (currentVersion == null) {
                    continue;
                }
                List<MavenVaadinVersion> allUpgrades = new ArrayList<MavenVaadinVersion>();
                MavenVaadinVersion newestUpgradeSameMinor = getLatestUpgrade(
                        currentVersion, availableVersions, true, false);
                if (newestUpgradeSameMinor != null) {
                    allUpgrades.add(newestUpgradeSameMinor);
                }
                MavenVaadinVersion newestStableUpgrade = getLatestUpgrade(
                        currentVersion, availableVersions, false, true);
                if (newestStableUpgrade != null
                        && !allUpgrades.contains(newestStableUpgrade)) {
                    allUpgrades.add(newestStableUpgrade);
                }
                // Suggest upgrading to the newest alpha/beta/rc version if
                // the current version is not a stable version
                if (!VersionUtil.isStableVersion(currentVersion)) {
                    MavenVaadinVersion newestUpgrade = getLatestUpgrade(
                            currentVersion, availableVersions, false, false);
                    if (newestUpgrade != null
                            && !allUpgrades.contains(newestUpgrade)) {
                        allUpgrades.add(newestUpgrade);
                    }
                }
                if (!allUpgrades.isEmpty()) {
                    availableUpgrades.put(project, allUpgrades);
                }
            } catch (CoreException e) {
                ErrorUtil.handleBackgroundException(IStatus.WARNING,
                        "Could not check Vaadin version in project " //$NON-NLS-1$
                                + project.getName(),
                        e);
            }
        }
        return availableUpgrades;
    }

    private MavenVaadinVersion getLatestUpgrade(String current,
            List<MavenVaadinVersion> availableVersions,
            boolean allowOnlySameMinor, boolean allowOnlyStableVersions) {
        String latestAsString = current;
        MavenVaadinVersion result = null;
        for (MavenVaadinVersion version : availableVersions) {
            String versionNumber = version.getVersionNumber();
            // Always require at least the major version to be the same as in
            // the current version.
            boolean acceptVersion = !allowOnlySameMinor
                    ? VersionUtil.isSameVersion(versionNumber, current, 1)
                    : VersionUtil.isSameVersion(versionNumber, current, 2);
            if (allowOnlyStableVersions) {
                acceptVersion = acceptVersion
                        && VersionUtil.isStableVersion(versionNumber);
            }
            if (acceptVersion && VersionUtil.compareVersions(versionNumber,
                    latestAsString) > 0) {
                result = version;
                latestAsString = version.getVersionNumber();
            }
        }
        return result;
    }

}