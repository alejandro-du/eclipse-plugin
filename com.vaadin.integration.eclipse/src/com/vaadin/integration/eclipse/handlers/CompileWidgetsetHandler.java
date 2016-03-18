package com.vaadin.integration.eclipse.handlers;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;

import com.vaadin.integration.eclipse.VaadinFacetUtils;
import com.vaadin.integration.eclipse.builder.WidgetsetBuildManager;
import com.vaadin.integration.eclipse.maven.MavenUtil;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.ProjectUtil;
import com.vaadin.integration.eclipse.util.WidgetsetUtil;

/**
 * Handler for Compile Widgetset action.
 *
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class CompileWidgetsetHandler extends AbstractVaadinCompileHandler {

    public CompileWidgetsetHandler() {
    }

    @Override
    public void startCompileJob(ISelection currentSelection,
            IEditorPart activeEditor) {
        startCompileWidgetsetJob(currentSelection, activeEditor, null);
    }

    public static void startCompileWidgetsetJob(
            final ISelection currentSelection, final IEditorPart activeEditor,
            ISchedulingRule schedulingRule) {
        Job job = new AbstractCompileJob("Compiling widgetset...", currentSelection, activeEditor) {

            @Override
            protected boolean handleMavenProject(IProject project) {
                return MavenUtil.compileWidgetSet(project);
            }

            @Override
            protected boolean compileSelectedFile(IProgressMonitor monitor,
                    IFile file) throws CoreException, IOException,
                    InterruptedException {
                IProject project = ProjectUtil.getProject(file);
                if (WidgetsetUtil.isWidgetsetManagedByPlugin(project)
                        && VaadinFacetUtils.isVaadinProject(project)) {
                    WidgetsetUtil.ensureWidgetsetNature(project);
                }
                return compileEditorOpenFile(monitor, file);
            }

            // Compile the only gwt.xml found in the selected project
            @Override
            protected boolean compileProject(IProgressMonitor monitor,
                    IProject project) throws CoreException, IOException,
                    InterruptedException {
                if (!VaadinFacetUtils.isVaadinProject(project)) {
                    return false;
                }
                return compileProject(monitor, project, true);
            }

            private boolean compileProject(IProgressMonitor monitor,
                    IProject project, boolean ensureNature)
                            throws CoreException, IOException, InterruptedException {
                if (WidgetsetUtil.isWidgetsetManagedByPlugin(project)) {
                    if (ensureNature) {
                        WidgetsetUtil.ensureWidgetsetNature(project);
                    }
                    IJavaProject jproject = JavaCore.create(project);
                    WidgetsetBuildManager.compileWidgetsets(jproject, monitor);
                    return true;
                }
                return false;
            }

            // Try to compile a file as a GWT widgetset, or if not one, try to
            // compile widgetsets in the containing project.
            @Override
            protected boolean compileEditorOpenFile(IProgressMonitor monitor, IFile file)
                    throws CoreException, IOException, InterruptedException {
                IProject project = ProjectUtil.getProject(file);
                if (!WidgetsetUtil.isWidgetsetManagedByPlugin(project)) {
                    return false;
                }
                // Upgrade facet version to 1.0 if was 0.1
                VaadinFacetUtils.fixFacetVersion(project);

                // only one branch is executed so progress is tracked correctly
                boolean compiled = false;
                if (file.getName().endsWith(".gwt.xml")
                        && file.getName().toLowerCase().contains("widgetset")) {
                    WidgetsetBuildManager.compileWidgetset(file, monitor);
                    compiled = true;
                }
                // plan B: compile the project containing the file
                if (!compiled && VaadinFacetUtils.isVaadinProject(project)) {
                    compiled = compileProject(monitor, project, false);
                }

                return compiled;
            }

            @Override
            protected void showNothingToCompileMessage() {
                ErrorUtil
                .displayErrorFromBackgroundThread("Select widgetset",
                        "Select a widgetset file (..widgetset.gwt.xml) or a Vaadin project to compile.");
            }

            @Override
            protected void showException(final Exception e) {
                ErrorUtil.displayErrorFromBackgroundThread("Error compiling widgetset",
                        "Error compiling widgetset:\n" + e.getClass().getName() + " - "
                                + e.getMessage() + "\n\nSee error log for details.");

                // Also log the exception
                ErrorUtil.handleBackgroundException(IStatus.ERROR,
                        "Widgetset compilation failed", e);
            }

        };

        if (schedulingRule != null) {
            job.setRule(schedulingRule);
        }
        job.setUser(false);
        job.schedule();
    }

}
