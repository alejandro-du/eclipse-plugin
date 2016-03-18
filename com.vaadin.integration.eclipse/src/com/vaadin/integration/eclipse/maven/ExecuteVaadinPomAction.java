package com.vaadin.integration.eclipse.maven;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.RefreshTab;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.internal.launch.Messages;
import org.eclipse.osgi.util.NLS;

import com.vaadin.integration.eclipse.util.ErrorUtil;

/**
 * Execute a POM action with workspace resolution enabled.
 * 
 * This class needs to duplicate logic from M2E ExecutePomAction as the related
 * methods are private there.
 */
public class ExecuteVaadinPomAction {

    public void launch(IProject project, String goal) {
        if (project == null) {
            return;
        }

        ILaunchConfiguration launchConfiguration = createLaunchConfiguration(
                project, goal);
        if (launchConfiguration == null) {
            return;
        }

        DebugUITools.launch(launchConfiguration, ILaunchManager.RUN_MODE);
    }

    private ILaunchConfiguration createLaunchConfiguration(IProject project,
            String goal) {
        try {
            ILaunchManager launchManager = DebugPlugin.getDefault()
                    .getLaunchManager();
            ILaunchConfigurationType launchConfigurationType = launchManager
                    .getLaunchConfigurationType(MavenLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID);

            String launchSafeGoalName = goal.replace(':', '-');

            ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType
                    .newInstance(
                            null, //
                            NLS.bind(Messages.ExecutePomAction_executing,
                                    launchSafeGoalName, project.getLocation()
                                            .toString().replace('/', '-')));
            workingCopy.setAttribute(MavenLaunchConstants.ATTR_POM_DIR, project
                    .getLocation().toOSString());
            workingCopy.setAttribute(MavenLaunchConstants.ATTR_GOALS, goal);
            workingCopy.setAttribute(IDebugUIConstants.ATTR_PRIVATE, true);
            workingCopy.setAttribute(RefreshTab.ATTR_REFRESH_SCOPE,
                    "${project}"); //$NON-NLS-1$
            workingCopy.setAttribute(RefreshTab.ATTR_REFRESH_RECURSIVE, true);

            // perform resolution of workspace artifacts
            workingCopy.setAttribute(
                    MavenLaunchConstants.ATTR_WORKSPACE_RESOLUTION, true);

            setProjectConfiguration(workingCopy, project);

            IPath path = getJREContainerPath(project);
            if (path != null) {
                workingCopy
                        .setAttribute(
                                IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH,
                                path.toPortableString());
            }

            return workingCopy;
        } catch (CoreException ex) {
            ErrorUtil.handleBackgroundException(ex);
        }
        return null;
    }

    private void setProjectConfiguration(
            ILaunchConfigurationWorkingCopy workingCopy, IProject project) {
        IMavenProjectRegistry projectManager = MavenPlugin
                .getMavenProjectRegistry();
        IFile pomFile = project
                .getFile(new Path(IMavenConstants.POM_FILE_NAME));
        IMavenProjectFacade projectFacade = projectManager.create(pomFile,
                false, new NullProgressMonitor());
        if (projectFacade != null) {
            ResolverConfiguration configuration = projectFacade
                    .getResolverConfiguration();

            String selectedProfiles = configuration.getSelectedProfiles();
            if (selectedProfiles != null && selectedProfiles.length() > 0) {
                workingCopy.setAttribute(MavenLaunchConstants.ATTR_PROFILES,
                        selectedProfiles);
            }
        }
    }

    // TODO ideally it should use MavenProject, but it is faster to scan
    // IJavaProjects
    private IPath getJREContainerPath(IProject project) throws CoreException {
        if (project != null && project.hasNature(JavaCore.NATURE_ID)) {
            IJavaProject javaProject = JavaCore.create(project);
            IClasspathEntry[] entries = javaProject.getRawClasspath();
            for (int i = 0; i < entries.length; i++) {
                IClasspathEntry entry = entries[i];
                if (JavaRuntime.JRE_CONTAINER
                        .equals(entry.getPath().segment(0))) {
                    return entry.getPath();
                }
            }
        }
        return null;
    }

}
