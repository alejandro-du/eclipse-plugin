package com.vaadin.integration.eclipse.maven;

import java.io.File;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;
import org.eclipse.m2e.jdt.AbstractSourcesGenerationProjectConfigurator;

/**
 * Project configurator that handles vaadin-maven-plugin specific tasks
 * (refresh, setting up the generated resource folder, ...).
 */
public class VaadinMavenProjectConfigurator extends
        AbstractSourcesGenerationProjectConfigurator {

    private static final String WIDGETSET_MODE_PARAMETER = "widgetsetMode";
    private static final String WIDGETSET_GENERATED_SOURCE_DIRECTORY_PARAMETER = "generatedSourceDirectory";
    private static final String WIDGETSET_GENERATED_RESOURCE_DIRECTORY = "generatedWidgetsetDirectory";

    private static final String WIDGETSET_MODE_VALUE_CDN = "cdn";
    private static final String WIDGETSET_MODE_VALUE_FETCH = "fetch";

    @Override
    public AbstractBuildParticipant getBuildParticipant( IMavenProjectFacade projectFacade,
                                                         MojoExecution execution,
                                                         IPluginExecutionMetadata executionMetadata )
    {
        return new VaadinMojoExecutionBuildParticipant(execution);
    }

    @Override
    protected File[] getSourceFolders(ProjectConfigurationRequest request,
            MojoExecution mojoExecution, IProgressMonitor monitor)
            throws CoreException {
        try {
            monitor.beginTask(
                    "Configuring generated source and resource folders", 2);

            String mode = getParameterValue(request.getMavenProject(),
                    WIDGETSET_MODE_PARAMETER, String.class, mojoExecution,
                    new SubProgressMonitor(monitor, 1));
            boolean isCdn = WIDGETSET_MODE_VALUE_CDN.equals(mode)
                    || WIDGETSET_MODE_VALUE_FETCH.equals(mode);

            if (VaadinMojoExecutionBuildParticipant.COMPILE_WIDGETSET_GOAL
                    .equals(mojoExecution.getGoal())) {
                return getCompileMojoSourceFolders(request, mojoExecution,
                        monitor, isCdn);
            } else if (VaadinMojoExecutionBuildParticipant.UPDATE_WIDGETSET_GOAL
                    .equals(mojoExecution
                    .getGoal())) {
                return getUpdateWidgetsetMojoSourceDirectories(request,
                        mojoExecution, monitor, isCdn);
            } else {
                // all other cases: skip updating the classpath
                return new File[0];
            }
        } finally {
            monitor.done();
        }
    }

    private File[] getCompileMojoSourceFolders(
            ProjectConfigurationRequest request, MojoExecution mojoExecution,
            IProgressMonitor monitor, boolean isCdn) throws CoreException {
        // add wscdn directory if using CDN mode
        if (isCdn) {
            // add the location of the generated sources for the
            // widgetset WebListener
            File widgetsetSourceDirectory = getParameterValue(
                    request.getMavenProject(),
                    WIDGETSET_GENERATED_SOURCE_DIRECTORY_PARAMETER, File.class,
                    mojoExecution, new SubProgressMonitor(monitor, 1));
            if (widgetsetSourceDirectory != null) {
                return new File[] { widgetsetSourceDirectory };
            } else {
                // fallback for earlier appwidgetset builds of the Maven
                // plug-in
                // TODO this can be removed after the public release of
                // the appwidgetset features
                widgetsetSourceDirectory = new File(request.getMavenProject()
                        .getBasedir(), "target/generated-sources/wscdn");
                if (widgetsetSourceDirectory != null) {
                    return new File[] { widgetsetSourceDirectory };
                }
            }
        }
        return new File[0];
    }

    private File[] getUpdateWidgetsetMojoSourceDirectories(
            ProjectConfigurationRequest request, MojoExecution mojoExecution,
            IProgressMonitor monitor, boolean isCdn) throws CoreException {
        // do not add the AppWidgetset.gwt.xml directory if using CDN
        if (!isCdn) {
            return super.getSourceFolders(request, mojoExecution,
                    new SubProgressMonitor(monitor, 1));
        }
        return new File[0];
    }

    @Override
    protected IPath getFullPath(IMavenProjectFacade facade, File file) {
        // older vaadin-maven-plugin versions do not have this parameter
        if (file == null) {
            return null;
        }
        return super.getFullPath(facade, file);
    }

    @Override
    protected String getOutputFolderParameterName() {
        return WIDGETSET_GENERATED_RESOURCE_DIRECTORY;
    }

}
