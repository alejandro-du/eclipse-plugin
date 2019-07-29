package com.vaadin.integration.eclipse.flow.wizard;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
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
import org.eclipse.ui.PlatformUI;

import com.vaadin.integration.eclipse.flow.util.LogUtil;

public class MavenGoal {

    private final String projectName;
    private final String goal;

    private final String mode = "run";

    public MavenGoal(String projectName, String goal) {
        this.projectName = projectName;
        this.goal = goal;
    }

    public void execute() {
        try {
            ILaunchConfiguration launchConfiguration = createLaunchConfiguration(
                    getProject());
            DebugUITools.launch(launchConfiguration, mode);
        } catch (CoreException e) {
            LogUtil.handleBackgroundException(e.getMessage(), e);
            LogUtil.displayError(
                    "Error occured during mvn package execution for the project",
                    null, PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                            .getShell());
        }
    }

    private IProject getProject() {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot root = workspace.getRoot();
        return (IProject) root.findMember(projectName);
    }

    private ILaunchConfiguration createLaunchConfiguration(IContainer basedir)
            throws CoreException {
        ILaunchManager launchManager = DebugPlugin.getDefault()
                .getLaunchManager();
        ILaunchConfigurationType launchConfigurationType = launchManager
                .getLaunchConfigurationType(
                        MavenLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID);

        String rawConfigName = NLS.bind(Messages.ExecutePomAction_executing,
                goal, basedir.getLocation().toString());
        String safeConfigName = launchManager
                .generateLaunchConfigurationName(rawConfigName);

        ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType
                .newInstance(null, safeConfigName);
        workingCopy.setAttribute(MavenLaunchConstants.ATTR_POM_DIR,
                basedir.getLocation().toOSString());
        workingCopy.setAttribute(MavenLaunchConstants.ATTR_GOALS, goal);
        workingCopy.setAttribute(IDebugUIConstants.ATTR_PRIVATE, true);
        workingCopy.setAttribute(RefreshTab.ATTR_REFRESH_SCOPE,
                "${resource:" + projectName + "}");
        workingCopy.setAttribute(RefreshTab.ATTR_REFRESH_RECURSIVE, true);

        setProjectConfiguration(workingCopy, basedir);

        IPath path = getJREContainerPath(basedir);
        if (path != null) {
            workingCopy.setAttribute(
                    IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH,
                    path.toPortableString());
        }
        return workingCopy;
    }

    private void setProjectConfiguration(
            ILaunchConfigurationWorkingCopy workingCopy, IContainer basedir) {
        IMavenProjectRegistry projectManager = MavenPlugin
                .getMavenProjectRegistry();
        IFile pomFile = basedir
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

    private IPath getJREContainerPath(IContainer basedir) throws CoreException {
        IProject project = basedir.getProject();
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
