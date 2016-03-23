package com.vaadin.integration.eclipse.maven;

import java.io.File;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
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
        // if there is no execution defined for update-widgetset, skip updating
        // the classpath
        if (!"update-widgetset".equals(mojoExecution.getGoal())) {
            return new File[0];
        }
        return super.getSourceFolders(request, mojoExecution, monitor);
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
        return "generatedWidgetsetDirectory";
    }

}
