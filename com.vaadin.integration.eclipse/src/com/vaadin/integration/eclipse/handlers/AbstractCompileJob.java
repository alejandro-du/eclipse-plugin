package com.vaadin.integration.eclipse.handlers;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;

import com.vaadin.integration.eclipse.maven.MavenUtil;
import com.vaadin.integration.eclipse.util.ProjectUtil;

/**
 * A base class for theme and widgetset compilation jobs.
 */
public abstract class AbstractCompileJob extends Job {

    private ISelection currentSelection;
    private IEditorPart activeEditor;

    protected AbstractCompileJob(String name,
            final ISelection currentSelection, final IEditorPart activeEditor) {
        super(name);

        this.currentSelection = currentSelection;
        this.activeEditor = activeEditor;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        try {
            monitor.beginTask(getName(), 1);

            IProject selectedProject = getSelectedProject(currentSelection,
                    activeEditor);
            boolean compiled = false;
            if (MavenUtil.isMavenProject(selectedProject)) {
                compiled = handleMavenProject(currentSelection);
            } else {
                compiled = handleIvyProject(currentSelection, activeEditor,
                        monitor);
            }

            // Nothing to compile found
            if (!compiled) {
                showNothingToCompileMessage();
            }
        } catch (OperationCanceledException e) {
            // Do nothing if user cancels compilation
        } catch (Exception e) {
            showException(e);
        } finally {
            monitor.done();
        }
        return Status.OK_STATUS;
    }

    /**
     * Perform compilation for an Ivy project
     * 
     * @param currentSelection
     *            current selection, can be null
     * @param activeEditor
     *            currently active editor, used if nothing suitable found based
     *            on selection
     * @param monitor
     *            progress monitor
     * @return true if compilation was performed, false otherwise
     * @throws CoreException
     * @throws IOException
     * @throws InterruptedException
     */
    protected boolean handleIvyProject(ISelection currentSelection,
            IEditorPart activeEditor, IProgressMonitor monitor)
            throws CoreException, IOException, InterruptedException {
        // TODO this is messy as this tries to preserve old behavior
        boolean compiled = false;

        // 1. Compile based on selected file
        IFile file = getSelectedFile(currentSelection);
        if (file != null) {
            compiled = compileSelectedFile(monitor, file);
        }

        // 2. Compile the project containing the selected resource
        if (!compiled) {
            IProject project = ProjectUtil.getProject(currentSelection);
            compiled = compileProject(monitor, project);
        }

        // 3. The project determined by the file open in the editor
        if (!compiled) {
            file = ProjectUtil.getFileForEditor(activeEditor);
            if (file != null && file.exists()
                    && ProjectUtil.getProject(file) != null) {
                compiled = compileEditorOpenFile(monitor, file);
            }
        }

        return compiled;
    }

    private static IFile getSelectedFile(ISelection currentSelection) {
        Object obj = ProjectUtil.getSingleSelection(currentSelection);
        if (obj instanceof IFile) {
            return (IFile) obj;
        }
        return null;
    }

    private static IProject getSelectedProject(ISelection currentSelection,
            IEditorPart activeEditor) {
        IProject project = ProjectUtil.getAnyProject(currentSelection);
        if (project != null) {
            return project;
        }

        IFile file = ProjectUtil.getFileForEditor(activeEditor);
        // this also handles the case where file is null
        return ProjectUtil.getProject(file);
    }

    /**
     * Show a message that the job found nothing to compile based on the current
     * selection and the active editor.
     */
    protected abstract void showNothingToCompileMessage();

    /**
     * Show an error message and optionally log a message about failed
     * compilation.
     * 
     * @param e
     *            exception from compilation
     */
    protected abstract void showException(final Exception e);

    /**
     * Compile a Maven project based on a selection.
     * 
     * @param current
     *            selection
     * @return true if a compilation process was triggered
     */
    protected abstract boolean handleMavenProject(ISelection currentSelection);

    // TODO the methods below and how the Ivy project compilation calls them are
    // somewhat messy, but kept this way to minimize behavioral changes from
    // past versions

    /**
     * Trigger compilation based on a single file selection.
     * 
     * This method is typically overridden by subclasses and called by
     * {@link #handleIvyProject(ISelection, IEditorPart, IProgressMonitor)}.
     * 
     * @param monitor
     *            progress monitor
     * @param file
     *            file to compile based on current selection
     * @return true if compilation was performed, false otherwise
     * @throws CoreException
     * @throws IOException
     * @throws InterruptedException
     */
    protected boolean compileSelectedFile(IProgressMonitor monitor, IFile file)
            throws CoreException, IOException, InterruptedException {
        // typically overridden and called by handleIvyProject
        return false;
    }

    /**
     * Trigger compilation based on a single file selection.
     * 
     * This method is typically overridden by subclasses and called by
     * {@link #handleIvyProject(ISelection, IEditorPart, IProgressMonitor)}.
     * 
     * @param monitor
     *            progress monitor
     * @param project
     *            project to compile, typically based on current selection if
     *            single file compilation was not performed
     * @return true if compilation was performed, false otherwise
     * @throws CoreException
     * @throws IOException
     * @throws InterruptedException
     */
    protected boolean compileProject(IProgressMonitor monitor,
            IProject project) throws CoreException, IOException,
            InterruptedException {
        // typically overridden and called by handleIvyProject
        return false;
    }
    
    /**
     * Trigger compilation based on a file that is open in an editor.
     * 
     * This method is typically overridden by subclasses and called by
     * {@link #handleIvyProject(ISelection, IEditorPart, IProgressMonitor)}.
     * 
     * @param monitor
     *            progress monitor
     * @param file
     *            file to compile based on active editor
     * @return true if compilation was performed, false otherwise
     * @throws CoreException
     * @throws IOException
     * @throws InterruptedException
     */
    protected boolean compileEditorOpenFile(
            IProgressMonitor monitor, IFile file)
            throws CoreException, IOException, InterruptedException {
        // typically overridden and called by handleIvyProject
        return false;
    }

}
