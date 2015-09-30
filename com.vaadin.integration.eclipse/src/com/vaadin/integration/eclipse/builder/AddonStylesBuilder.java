package com.vaadin.integration.eclipse.builder;

import java.io.IOException;
import java.util.Map;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import com.vaadin.integration.eclipse.util.ProjectUtil;

public class AddonStylesBuilder extends IncrementalProjectBuilder {

    public static final String BUILDER_ID = "com.vaadin.integration.eclipse.addonStylesBuilder";

    class AddonStyleDeltaVisitor implements IResourceDeltaVisitor {
        private IProgressMonitor monitor;

        public AddonStyleDeltaVisitor(IProgressMonitor monitor) {
            this.monitor = monitor;
        }

        private boolean isJar(IResource resource) {
            return resource instanceof IFile
                    && resource.getName().endsWith(".jar");
        }

        public boolean visit(IResourceDelta delta) throws CoreException {
            IResource resource = delta.getResource();
            int kind = delta.getKind();

            if (kind == IResourceDelta.ADDED || kind == IResourceDelta.CHANGED) {
                if (isAddonPackageWithStyles(resource.getRawLocation())) {
                    run(monitor);
                }
            } else if (kind == IResourceDelta.REMOVED && isJar(resource)) {
                run(monitor);
            }

            // return true to continue visiting children.
            return true;
        }
    }

    @Override
    protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
            throws CoreException {
        if (kind == FULL_BUILD) {
            run(monitor);
        } else {
            IResourceDelta delta = getDelta(getProject());
            if (delta == null) {
                run(monitor);
            } else {
                delta.accept(new AddonStyleDeltaVisitor(monitor));
            }
        }
        return null;
    }

    private void run(final IProgressMonitor monitor) throws CoreException {
        IProject project = getProject();
        if (AddonStylesImporter.isSupported(project)) {
            IFolder themes = ProjectUtil.getThemesFolder(project);
            if (themes.exists()) {
                for (IResource theme : themes.members()) {
                    IFolder themeFolder = (IFolder) theme;
                    try {
                        AddonStylesImporter.run(project, monitor, themeFolder);
                        themeFolder.refreshLocal(IResource.DEPTH_INFINITE,
                                new SubProgressMonitor(monitor, 1));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static void addBuilder(IProject project) throws CoreException {
        IProjectDescription desc = project.getDescription();
        ICommand[] commands = desc.getBuildSpec();

        for (int i = 0; i < commands.length; ++i) {
            if (commands[i].getBuilderName().equals(
                    AddonStylesBuilder.BUILDER_ID)) {
                return;
            }
        }

        ICommand[] newCommands = new ICommand[commands.length + 1];
        System.arraycopy(commands, 0, newCommands, 0, commands.length);
        ICommand command = desc.newCommand();
        command.setBuilderName(AddonStylesBuilder.BUILDER_ID);
        newCommands[newCommands.length - 1] = command;
        desc.setBuildSpec(newCommands);
        project.setDescription(desc, null);
    }

    public static void removeBuilder(IProject project) throws CoreException {
        IProjectDescription desc = project.getDescription();
        ICommand[] commands = desc.getBuildSpec();
        int builderIndex = -1;
        for (int i = 0; i < commands.length; i++) {
            if (commands[i].getBuilderName().equals(
                    AddonStylesBuilder.BUILDER_ID)) {
                builderIndex = i;
                break;
            }
        }
        if (builderIndex != -1) {
            ICommand[] newCommands = new ICommand[commands.length - 1];
            System.arraycopy(commands, 0, newCommands, 0, builderIndex);
            System.arraycopy(commands, builderIndex + 1, newCommands,
                    builderIndex, (commands.length - 1) - builderIndex);
            desc.setBuildSpec(newCommands);
            project.setDescription(desc, null);
        }
    }

    private static boolean isAddonPackageWithStyles(IPath resource) {
        return ProjectUtil.hasManifestAttribute("Vaadin-Stylesheets", resource);
    }
}
