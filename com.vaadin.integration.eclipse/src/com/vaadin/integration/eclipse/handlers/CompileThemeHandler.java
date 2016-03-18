package com.vaadin.integration.eclipse.handlers;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;

import com.vaadin.integration.eclipse.VaadinFacetUtils;
import com.vaadin.integration.eclipse.builder.ThemeCompiler;
import com.vaadin.integration.eclipse.maven.MavenUtil;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.ProjectUtil;

/**
 * Handler for Compile Theme action.
 *
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class CompileThemeHandler extends AbstractVaadinCompileHandler {

    public CompileThemeHandler() {
    }

    @Override
    public void startCompileJob(ISelection currentSelection,
            IEditorPart activeEditor) {
        startCompileThemeJob(currentSelection, activeEditor, null);
    }

    public static void startCompileThemeJob(final ISelection currentSelection,
            final IEditorPart activeEditor, ISchedulingRule schedulingRule) {
        Job job = new AbstractCompileJob("Compiling theme...", currentSelection, activeEditor) {

            @Override
            protected boolean handleMavenProject(IProject project) {
                return MavenUtil.compileTheme(project);
            }

            @Override
            protected boolean compileSelectedFile(IProgressMonitor monitor,
                    IFile file) throws CoreException, IOException,
                    InterruptedException {
                return compileEditorOpenFile(monitor, file);
            }

            // compile all the themes in the project
            @Override
            protected boolean compileProject(IProgressMonitor monitor,
                    IProject project) throws CoreException {
                if (!VaadinFacetUtils.isVaadinProject(project)) {
                    return false;
                }
                IFolder themes = ProjectUtil.getThemesFolder(project);
                if (themes.exists()) {
                    for (IResource theme : themes.members()) {
                        if (theme instanceof IFolder) {
                            IFolder themeFolder = (IFolder) theme;
                            try {
                                ThemeCompiler.run(project, monitor, themeFolder);
                                themeFolder.refreshLocal(IResource.DEPTH_ONE,
                                        new SubProgressMonitor(monitor, 1));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                return true;
            }

            // Try to compile a file as an SCSS theme, or if not one, try to
            // compile themes in the containing project
            @Override
            protected boolean compileEditorOpenFile(IProgressMonitor monitor, IFile file)
                    throws CoreException, IOException, InterruptedException {
                if (null == file || null == file.getProject()) {
                    return false;
                }

                IProject project = file.getProject();
                IFolder themes = ProjectUtil.getThemesFolder(project);

                if (!themes.exists()) {
                    return false;
                }

                boolean compiled = false;

                // TODO could check here if the file is within a theme and only compile
                // that theme

                if (!compiled) {
                    if (VaadinFacetUtils.isVaadinProject(project)) {
                        compiled = compileProject(monitor, project);
                    }
                }

                return compiled;
            }

            @Override
            protected void showNothingToCompileMessage() {
                ErrorUtil
                .displayErrorFromBackgroundThread("Select theme",
                        "Select a theme file (.scss) or a Vaadin project to compile.");
            }

            @Override
            protected void showException(final Exception e) {
                ErrorUtil.displayErrorFromBackgroundThread(
                        "Error compiling theme",
                        "Error compiling theme:\n" + e.getClass().getName() + " - "
                                + e.getMessage() + "\n\nSee error log for details.");

                // Also log the exception
                ErrorUtil.handleBackgroundException(IStatus.ERROR,
                        "Theme compilation failed", e);
            }

        };

        if (schedulingRule != null) {
            job.setRule(schedulingRule);
        }
        job.setUser(false);
        job.schedule();
    }

}
