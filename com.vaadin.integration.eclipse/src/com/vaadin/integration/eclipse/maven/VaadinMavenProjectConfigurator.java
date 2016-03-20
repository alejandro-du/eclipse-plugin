package com.vaadin.integration.eclipse.maven;
import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;

/**
 * Project configurator that handles vaadin-maven-plugin specific tasks
 * (refresh, setting up the generated resource folder, ...).
 */
public class VaadinMavenProjectConfigurator extends AbstractProjectConfigurator {

    @Override
    public AbstractBuildParticipant getBuildParticipant( IMavenProjectFacade projectFacade,
                                                         MojoExecution execution,
                                                         IPluginExecutionMetadata executionMetadata )
    {
        return new VaadinMojoExecutionBuildParticipant(execution);
    }

    @Override
    public void configure(ProjectConfigurationRequest request,
            IProgressMonitor monitor) throws CoreException {
        // nothing to do for now
    }

}
