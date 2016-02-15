package com.vaadin.integration.eclipse.handlers;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;

import com.vaadin.integration.eclipse.maven.MavenUtil;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.ProjectUtil;

/**
 * Handler for Compile Theme and Widgetset action.
 *
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class CompileThemeAndWidgetsetHandler extends AbstractVaadinCompileHandler {

    public CompileThemeAndWidgetsetHandler() {
    }

    @Override
    public void startCompileJob(ISelection currentSelection,
            IEditorPart activeEditor) {
        startCompileThemeAndWidgetsetJob(currentSelection, activeEditor, null);
    }

    public static void startCompileThemeAndWidgetsetJob(
            final ISelection currentSelection, final IEditorPart activeEditor,
            ISchedulingRule schedulingRule) {
        Job job = new AbstractCompileJob("Compiling theme and widgetset...",
                currentSelection, activeEditor) {

            @Override
            protected boolean handleMavenProject(ISelection currentSelection) {
                return MavenUtil.compileThemeAndWidgetset(currentSelection);
            }

            @Override
            protected boolean handleIvyProject(ISelection currentSelection,
                    IEditorPart activeEditor, IProgressMonitor monitor)
                    throws CoreException, IOException, InterruptedException {
                // trigger separate jobs as before in Ivy projects
                IProject project = ProjectUtil.getProject(currentSelection,
                        activeEditor);
                CompileThemeHandler.startCompileThemeJob(currentSelection,
                        activeEditor, project);
                CompileWidgetsetHandler.startCompileWidgetsetJob(
                        currentSelection, activeEditor, project);
                return true;
            }

            @Override
            protected void showNothingToCompileMessage() {
                ErrorUtil.displayErrorFromBackgroundThread("Select project",
                        "Select a Vaadin project to compile.");
            }

            @Override
            protected void showException(final Exception e) {
                ErrorUtil.displayErrorFromBackgroundThread(
                        "Error compiling theme or widgetset",
                        "Error compiling:\n" + e.getClass().getName() + " - "
                                + e.getMessage() + "\n\nSee error log for details.");

                // Also log the exception
                ErrorUtil.handleBackgroundException(IStatus.ERROR,
                        "Theme or widgetset compilation failed", e);
            }

        };

        if (schedulingRule != null) {
            job.setRule(schedulingRule);
        }
        job.setUser(false);
        job.schedule();
    }

}
