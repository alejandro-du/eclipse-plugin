package com.vaadin.integration.eclipse;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.vaadin.integration.eclipse.builder.AddonStylesBuilder;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.PreferenceUtil;
import com.vaadin.integration.eclipse.util.ProjectUtil;

public class CheckAddonStylesBuilderJob extends Job {

    private IProject project;

    public CheckAddonStylesBuilderJob(String name, IProject projectToCheck) {
        super(name);
        project = projectToCheck;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        if (ProjectUtil.isVaadin7(project)) {
            // Activate or deactivate AddonStylesBuilder depending on
            // project settings
            PreferenceUtil prefUtil = PreferenceUtil.get(project);
            try {
                if (prefUtil.isAddonThemeScanningSuspended()) {
                    AddonStylesBuilder.removeBuilder(project);
                } else {
                    AddonStylesBuilder.addBuilder(project);
                }
            } catch (CoreException e) {
                ErrorUtil.handleBackgroundException(e);
            }
        }
        return Status.OK_STATUS;
    }
}