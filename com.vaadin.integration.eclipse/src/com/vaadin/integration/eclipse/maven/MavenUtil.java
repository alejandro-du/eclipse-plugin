package com.vaadin.integration.eclipse.maven;

import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

public class MavenUtil {

    private static final String VAADIN_MAVEN_PLUGIN_GROUP_ID = "com.vaadin";
    private static final String VAADIN_MAVEN_PLUGIN_ARTIFACT_ID = "vaadin-maven-plugin";
    private static final String VAADIN_MAVEN_PLUGIN_PREFIX = "vaadin:";

    public static boolean isMavenProject(IProject project) {
        if (project == null) {
            return false;
        }

        try {
            return project.hasNature(IMavenConstants.NATURE_ID);
        } catch (CoreException e) {
            return false;
        }
    }

    public static void runMavenGoal(final IProject project,
            final String goal) {
        Display display = PlatformUI.getWorkbench().getDisplay();
        if (!display.isDisposed()) {
            // this needs to be done in the UI thread and will trigger a
            // background job
            display.asyncExec(new Runnable() {
                public void run() {
                    ExecuteVaadinPomAction exec = new ExecuteVaadinPomAction();
                    exec.launch(project, goal);
                }
            });
        }
    }

    /**
     * Check whether a Maven project has a specific plug-in execution
     * configured.
     * 
     * @param project
     *            Maven project, not null
     * @param pluginGroupId
     *            Maven plug-in groupId to look for
     * @param pluginArtifactId
     *            Maven plug-in artifactIdd to look for
     * @param goal
     *            the Maven goal that whose execution is being looked for
     * @return true if the project has the goal configured in the executions
     *         list of the given plug-in
     */
    private static boolean definesPluginExecution(MavenProject project,
            String pluginGroupId, String pluginArtifactId, String goal) {
        if (project.getOriginalModel().getBuild() == null) {
            return false;
        }
        for (Plugin p : project.getOriginalModel().getBuild().getPlugins()) {
            if (p.getGroupId().equals(pluginGroupId)
                    && p.getArtifactId().equals(pluginArtifactId)) {
                for (PluginExecution execution : p.getExecutions()) {
                    if (execution.getGoals().contains(goal)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static IMavenProjectFacade getMavenProjectFacade(IProject project) {
        IMavenProjectRegistry projectManager = MavenPlugin
                .getMavenProjectRegistry();
        IFile pomFile = project
                .getFile(new Path(IMavenConstants.POM_FILE_NAME));
        IMavenProjectFacade projectFacade = projectManager.create(pomFile,
                false, new NullProgressMonitor());
        return projectFacade;
    }

    private static boolean runMavenGoals(IProject project,
            String pluginGroupId,
            String pluginArtifactId, String prefix, String... goals)
            throws CoreException {
        IMavenProjectFacade facade = getMavenProjectFacade(project);
        MavenProject mavenProject = facade
                .getMavenProject(new NullProgressMonitor());
        String enabledGoals = "";
        String allGoals = "";
        for (String goal : goals)
         {
            allGoals = allGoals + prefix + goal + " ";
            if (definesPluginExecution(mavenProject, pluginGroupId,
                    pluginArtifactId, goal)) {
                enabledGoals = enabledGoals + prefix + goal
                        + " ";
            }
        }
        if (!enabledGoals.isEmpty()) {
            runMavenGoal(project, enabledGoals);
            return true;
        } else if (!facade.getMavenProjectModules().isEmpty()) {
            // iterate over subprojects and do the same
            boolean executed = false;
            IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace()
                    .getRoot();
            // find sub-projects
            IProject[] projects = workspaceRoot.getProjects();
            for (IProject p : projects) {
                if (project.isOpen() && isMavenProject(p)) {
                    if (project.getLocation().isPrefixOf(p.getLocation())
                            && !p.equals(project)) {
                        executed = runMavenGoals(p, pluginGroupId,
                                pluginArtifactId, prefix, goals) || executed;
                    }
                }
            }
            return executed;
        } else {
            return false;
        }
    }

    public static boolean compileWidgetSet(IProject project) {
        try {
            if (!runMavenGoals(project, VAADIN_MAVEN_PLUGIN_GROUP_ID,
                    VAADIN_MAVEN_PLUGIN_ARTIFACT_ID,
                    VAADIN_MAVEN_PLUGIN_PREFIX, "update-widgetset", "compile")) {
                // fallback: run in the selected project and hope it works
                runMavenGoal(project, "vaadin:update-widgetset vaadin:compile");
            }
            return true;
        } catch (CoreException e) {
            return false;
        }
    }

    public static boolean compileTheme(IProject project) {
        try {
            if (!runMavenGoals(project, VAADIN_MAVEN_PLUGIN_GROUP_ID,
                    VAADIN_MAVEN_PLUGIN_ARTIFACT_ID,
                    VAADIN_MAVEN_PLUGIN_PREFIX, "update-theme", "compile-theme")) {
                // fallback: run in the selected project and hope it works
                runMavenGoal(project,
                        "vaadin:update-theme vaadin:compile-theme");
            }
            return true;
        } catch (CoreException e) {
            return false;
        }
    }

    public static boolean compileThemeAndWidgetset(IProject project) {
        try {
            if (!runMavenGoals(project, VAADIN_MAVEN_PLUGIN_GROUP_ID,
                    VAADIN_MAVEN_PLUGIN_ARTIFACT_ID,
                    VAADIN_MAVEN_PLUGIN_PREFIX, "update-theme",
                    "update-widgetset", "compile-theme", "compile")) {
                // fallback: run in the selected project and hope it works
                runMavenGoal(
                        project,
                        "vaadin:update-theme vaadin:update-widgetset vaadin:compile-theme vaadin:compile");
            }
            return true;
        } catch (CoreException e) {
            return false;
        }
    }
}
