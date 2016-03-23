package com.vaadin.integration.eclipse.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import com.vaadin.integration.eclipse.builder.WidgetsetBuildManager;
import com.vaadin.integration.eclipse.util.data.LocalVaadinVersion;
import com.vaadin.integration.eclipse.util.files.LocalFileManager;
import com.vaadin.integration.eclipse.util.network.DownloadManager;
import com.vaadin.integration.eclipse.variables.VaadinClasspathVariableInitializer;

public class ProjectDependencyManager {
    /**
     * Ensure that some Vaadin jar file can be found in the project. If none can
     * be found, adds the specified version from the local repository.
     * 
     * No launch configurations are updated. Use updateVaadinLibraries if such
     * updates are needed.
     * 
     * Requests to the user for widgetset builds in the project are suspended
     * for the duration of this operation and resumed after completion. At the
     * end, the user is asked about compiling the widgetset if it is dirty.
     * 
     * @param project
     * @param vaadinJarVersion
     * @param monitor
     * @throws CoreException
     */
    public static void ensureVaadinLibraries(IProject project,
            LocalVaadinVersion vaadinVersion, IProgressMonitor monitor)
            throws CoreException {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        try {
            monitor.beginTask(
                    "Ensuring the project includes the Vaadin library", 1);

            IJavaProject jproject = JavaCore.create(project);
            try {
                IType findType = ProjectUtil
                        .findVaadinApplicationType(jproject);
                if (findType == null) {
                    WidgetsetBuildManager
                            .internalSuspendWidgetsetBuilds(project);
                    try {
                        ProjectDependencyManager.addVaadinLibrary(jproject,
                                vaadinVersion, new SubProgressMonitor(monitor,
                                        1));

                        // refresh library folder to recompile parts of project
                        IFolder lib = ProjectUtil.getWebInfLibFolder(project);
                        if (lib != null && lib.exists()) {
                            // should exist after adding the library
                            lib.refreshLocal(IResource.DEPTH_ONE, null);
                        }
                    } finally {
                        WidgetsetBuildManager
                                .internalResumeWidgetsetBuilds(project);
                        if (WidgetsetUtil.isWidgetsetDirty(project)) {
                            WidgetsetBuildManager.runWidgetSetBuildTool(
                                    project, false, new NullProgressMonitor());
                        }
                    }
                }
            } catch (JavaModelException e) {
                throw ErrorUtil
                        .newCoreException(
                                "Failed to ensure that a Vaadin jar is included in project",
                                e);
            }
        } finally {
            monitor.done();
        }
    }

    /**
     * Update the Vaadin jar file in the project. If the project already
     * contains a Vaadin jar, it is removed.
     * 
     * Update widgetset compilation launch configurations in the project to
     * refer to the new Vaadin and GWT versions (only when changing Vaadin
     * version, not when adding the JAR).
     * 
     * Requests to the user for widgetset builds in the project are suspended
     * for the duration of this operation and resumed after completion. At the
     * end, the user is asked about compiling the widgetset if it is dirty.
     * 
     * @param project
     * @param newLocalVaadinJarVersion
     *            or null to remove current Vaadin library
     * @throws CoreException
     */
    public static void updateVaadinLibraries(IProject project,
            LocalVaadinVersion newLocalVaadinJarVersion,
            IProgressMonitor monitor) throws CoreException {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        try {
            monitor.beginTask("Updating Vaadin libraries in the project", 12);

            boolean searchOutsideWebinfLib = true;
            // do nothing if correct version is already in the project
            IPath currentJar = ProjectUtil.getVaadinLibraryInProject(project,
                    searchOutsideWebinfLib);
            String currentVersion = VersionUtil
                    .getVaadinVersionFromJar(currentJar);

            IJavaProject jproject = JavaCore.create(project);
            WidgetsetBuildManager.internalSuspendWidgetsetBuilds(project);
            try {
                // replace the Vaadin JAR (currentVersion) with the new one
                if (currentJar != null) {
                    removeVaadinLibrary(jproject, currentJar);
                }
                monitor.worked(1);
                if (newLocalVaadinJarVersion != null) {
                    addVaadinLibrary(jproject, newLocalVaadinJarVersion,
                            new SubProgressMonitor(monitor, 9));
                }
                // refresh library folder to recompile parts of project
                IFolder lib = ProjectUtil.getWebInfLibFolder(project);
                if (lib == null) {
                    ErrorUtil
                            .logInfo("Could not add Vaadin libraries to the project "
                                    + project.getName()
                                    + ". Possibly not a Dynamic Web Project.");
                    return;
                }
                if (lib.exists()) {
                    // should exist at least if added to the project
                    lib.refreshLocal(IResource.DEPTH_ONE, null);
                }

                // TODO also handle adding Vaadin JAR to a project if the user
                // has removed it and adds a different version?
                if (currentVersion != null && newLocalVaadinJarVersion != null) {
                    // update launches
                    String oldVaadinJarName = VersionUtil
                            .getVaadinJarFilename(currentVersion);
                    String newVaadinJarName = newLocalVaadinJarVersion
                            .getJarFilename();

                    // this is safer than findProjectVaadinJarPath() as we may
                    // be in the process of changing the classpath
                    IPath vaadinJarPath = ProjectUtil
                            .getWebInfLibFolder(project)
                            .getFile(newVaadinJarName).getFullPath();
                    VaadinPluginUtil.updateLaunchClassPath(project,
                            new String[] { oldVaadinJarName }, vaadinJarPath);
                    monitor.worked(1);

                    // check if Vaadin JAR is explicitly on classpath instead of
                    // through the "magic" classpath container for WEB-INF/lib
                    // => update classpath reference
                    IClasspathEntry[] rawClasspath = jproject.getRawClasspath();
                    List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
                    for (IClasspathEntry entry : rawClasspath) {
                        entries.add(entry);
                    }

                    IClasspathEntry vaadinEntry = JavaCore.newLibraryEntry(
                            vaadinJarPath, null, null);

                    // replace explicit reference to the Vaadin JAR if found,
                    // otherwise do nothing
                    VaadinPluginUtil.replaceClassPathEntry(entries,
                            vaadinEntry, new String[] { oldVaadinJarName },
                            false);

                    IClasspathEntry[] entryArray = entries
                            .toArray(new IClasspathEntry[entries.size()]);
                    jproject.setRawClasspath(entryArray, null);

                    monitor.worked(1);
                }
            } catch (JavaModelException e) {
                throw ErrorUtil.newCoreException(
                        "Failed to update Vaadin jar in project", e);
            } finally {
                WidgetsetBuildManager.internalResumeWidgetsetBuilds(project);
                if (WidgetsetUtil.isWidgetsetDirty(project)) {
                    WidgetsetBuildManager.runWidgetSetBuildTool(project, false,
                            new NullProgressMonitor());
                }
            }
        } finally {
            monitor.done();
        }
    }

    /**
     * Adds the specified Vaadin jar version from the local store to the
     * project. The specified version must be found from the local store or an
     * exception is thrown.
     * 
     * @param jproject
     * @param vaadinVersion
     * @param monitor
     * @throws CoreException
     */
    private static void addVaadinLibrary(IJavaProject jproject,
            LocalVaadinVersion vaadinVersion, IProgressMonitor monitor)
            throws CoreException {

        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        try {
            monitor.beginTask(
                    "Adding Vaadin required libraries to the project", 10);

            IProject project = jproject.getProject();
            IFolder lib = ProjectUtil.getWebInfLibFolder(project);
            if (lib == null) {
                ErrorUtil
                        .displayWarningFromBackgroundThread(
                                "Could not add Vaadin JAR",
                                "The project "
                                        + project.getName()
                                        + " does not seem to have the Dynamic Web Module facet.");
                return;
            }
            if (!lib.exists()) {
                VaadinPluginUtil.createFolders(lib, monitor);
            }
            IFile targetFile = lib.getFile(vaadinVersion.getJarFilename());
            IPath sourceFile = vaadinVersion.getJarFile();
            VaadinPluginUtil.copyPluginFileToProject(sourceFile, targetFile);

            // refresh project
            lib.refreshLocal(IResource.DEPTH_ONE, new SubProgressMonitor(
                    monitor, 1));

            // make sure the GWT library versions match the Vaadin JAR
            // requirements

            String gwtVersion = ProjectUtil
                    .getRequiredGWTVersionForVaadinJar(targetFile.getLocation());

            if (gwtVersion != null) {
                monitor.worked(1);
                List<String> dependencies = VersionUtil
                        .getRequiredGWTDependenciesForVaadinJar(targetFile
                                .getLocation());

                updateGWTLibraries(jproject, gwtVersion, dependencies,
                        new SubProgressMonitor(monitor, 4));
            } else {
                monitor.worked(5);
            }
        } catch (Exception e) {
            throw ErrorUtil.newCoreException(
                    "Failed to add Vaadin jar to project", e);
        } finally {
            monitor.done();
        }
    }

    /**
     * Removes the specified Vaadin jar version from the project (if it exists).
     * A CoreException is thrown if the jar file is outside the project.
     * 
     * @param jproject
     * @param currentJar
     * @throws CoreException
     */
    public static void removeVaadinLibrary(IJavaProject jproject,
            IPath currentJar) throws CoreException {
        try {
            IProject project = jproject.getProject();
            if (!ProjectUtil.isInProject(project, currentJar)) {
                throw ErrorUtil
                        .newCoreException("Cannot remove Vaadin jar outside the project");
            }

            IWorkspace workspace = project.getWorkspace();
            // IPath location = Path.fromOSString(currentJar.toOSString());
            IFile file = workspace.getRoot().getFileForLocation(currentJar);

            if (file == null || !file.exists()) {
                throw ErrorUtil
                        .newCoreException("Failed to remove old Vaadin jar ("
                                + currentJar.toOSString() + ")");
            }

            file.delete(true, null);

            // refresh parent directory
            file.getParent().refreshLocal(IResource.DEPTH_ONE, null);
        } catch (Exception e) {
            throw ErrorUtil.newCoreException(
                    "Failed to remove Vaadin jar from project", e);
        }
    }

    /**
     * Ensure that the project classpath contains the GWT libraries, adding them
     * if necessary.
     * 
     * Also update widgetset compilation launch configuration paths as needed.
     * 
     * Requests to the user for widgetset builds in the project are suspended
     * for the duration of this operation and resumed after completion. At the
     * end, the user is asked about compiling the widgetset if it is dirty.
     * 
     * @param project
     * @param monitor
     * @throws CoreException
     */
    public static void ensureGWTLibraries(IProject project,
            IProgressMonitor monitor) throws CoreException {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        try {
            monitor.beginTask(
                    "Ensuring that the project classpath contains GWT libraries",
                    2);

            IJavaProject jproject = JavaCore.create(project);
            try {
                IType findType = jproject
                        .findType("com.google.gwt.core.client.EntryPoint");

                if (findType == null) {
                    WidgetsetBuildManager
                            .internalSuspendWidgetsetBuilds(project);
                    try {
                        String gwtVersion = ProjectUtil
                                .getRequiredGWTVersionForProject(jproject);

                        if (gwtVersion != null) {
                            monitor.worked(1);
                            List<String> gwtDependencies = ProjectUtil
                                    .getRequiredGWTDependenciesForProject(jproject);
                            updateGWTLibraries(jproject, gwtVersion,
                                    gwtDependencies, new SubProgressMonitor(
                                            monitor, 1));
                        } else {
                            monitor.worked(2);
                        }

                    } finally {
                        WidgetsetBuildManager
                                .internalResumeWidgetsetBuilds(project);
                        if (WidgetsetUtil.isWidgetsetDirty(project)) {
                            WidgetsetBuildManager.runWidgetSetBuildTool(
                                    project, false, new NullProgressMonitor());
                        }
                    }
                }
            } catch (JavaModelException e) {
                throw ErrorUtil
                        .newCoreException(
                                "Failed to ensure GWT libraries are present in the project",
                                e);
            }
        } finally {
            monitor.done();
        }
    }

    /**
     * Download and add or update GWT libraries in a project based on the Vaadin
     * version in the project (if any).
     * 
     * The project build path and any external launches (including the widgetset
     * compilation launch for Vaadin 6.1 or earlier) are also updated.
     * 
     * If the project build path contains user-defined GWT JARs, neither the
     * build path nor the launches are modified.
     * 
     * @param jproject
     * @param gwtDependencies
     * @param monitor
     * @throws CoreException
     */
    private static void updateGWTLibraries(IJavaProject jproject,
            String gwtVersion, List<String> gwtDependencies,
            IProgressMonitor monitor) throws CoreException {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        try {
            monitor.beginTask("Updating GWT libraries", 13);

            // do not replace the GWT JARs directly on the build path and in
            // launches if they are user-defined
            // this ignores JARs in WEB-INF/lib, which could be temporary for
            // OOPHM, and does modify the top-level classpath entries - it is up
            // to the user to ensure the correct classpath order in such cases
            if (isUsingUserDefinedGwt(jproject, false)) {
                return;
            }

            try {
                DownloadManager.downloadGwtUserJar(gwtVersion,
                        new SubProgressMonitor(monitor, 5));
                DownloadManager.downloadGwtDevJar(gwtVersion,
                        new SubProgressMonitor(monitor, 5));

                for (String dependency : gwtDependencies) {
                    DownloadManager.downloadDependency(gwtVersion, dependency,
                            new SubProgressMonitor(monitor, 5));
                }

                IClasspathEntry[] rawClasspath = jproject.getRawClasspath();
                List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
                for (IClasspathEntry entry : rawClasspath) {
                    boolean add = true;
                    // Skip GWT dependencies. They are re-added below if needed
                    IPath p = entry.getResolvedEntry().getPath();
                    if (ProjectUtil.isGWTDependency(jproject, p)) {
                        add = false;
                    }
                    if (add) {
                        entries.add(entry);
                    }
                }

                Map<String, IPath> jarToLocation = new HashMap<String, IPath>();

                // use the VAADIN_DOWNLOAD_VARIABLE variable and variable
                // classpath entries where feasible

                /* GWT-DEV */
                IPath devJarPath = LocalFileManager
                        .getLocalGwtDevJar(gwtVersion);
                IClasspathEntry gwtDev = VaadinPluginUtil
                        .makeVariableClasspathEntry(
                                VaadinClasspathVariableInitializer.VAADIN_DOWNLOAD_VARIABLE,
                                devJarPath);
                // replace gwt-dev-[platform].jar and/or gwt-dev.jar if found,
                // otherwise append new entry
                String devJarName = "gwt-dev-" + PlatformUtil.getPlatform()
                        + ".jar";
                VaadinPluginUtil.replaceClassPathEntry(entries, gwtDev,
                        new String[] { "gwt-dev.jar", devJarName }, true);

                /* GWT-USER */
                IPath userJarPath = LocalFileManager
                        .getLocalGwtUserJar(gwtVersion);
                IClasspathEntry gwtUser = VaadinPluginUtil
                        .makeVariableClasspathEntry(
                                VaadinClasspathVariableInitializer.VAADIN_DOWNLOAD_VARIABLE,
                                userJarPath);
                // replace gwt-user.jar if found, otherwise append new entry
                VaadinPluginUtil.replaceClassPathEntry(entries, gwtUser,
                        new String[] { "gwt-user.jar" }, true);

                /* GWT dependencies */
                for (String dependencyJar : gwtDependencies) {
                    IPath dependencyPath = LocalFileManager
                            .getLocalGWTDependencyJar(gwtVersion, dependencyJar);
                    IClasspathEntry dep = VaadinPluginUtil
                            .makeVariableClasspathEntry(
                                    VaadinClasspathVariableInitializer.VAADIN_DOWNLOAD_VARIABLE,
                                    dependencyPath);

                    // replace dep jars if found, otherwise append new entry
                    VaadinPluginUtil.replaceClassPathEntry(entries, dep,
                            new String[] { dependencyJar }, true);

                    jarToLocation.put(dependencyJar, dependencyPath);
                }

                IClasspathEntry[] entryArray = entries
                        .toArray(new IClasspathEntry[entries.size()]);
                jproject.setRawClasspath(entryArray, null);

                monitor.worked(1);

                IProject project = jproject.getProject();

                // update classpaths also in launches
                if (!isUsingUserDefinedGwt(jproject, true)) {
                    VaadinPluginUtil.updateLaunchClassPath(project,
                            new String[] { "gwt-dev.jar", devJarName },
                            devJarPath);

                    VaadinPluginUtil.updateLaunchClassPath(project,
                            new String[] { "gwt-user.jar" }, userJarPath);

                    monitor.worked(1);

                    for (String jarName : jarToLocation.keySet()) {
                        VaadinPluginUtil.updateLaunchClassPath(project,
                                new String[] { jarName },
                                jarToLocation.get(jarName));
                    }
                    monitor.worked(1);

                } else {
                    monitor.worked(2);
                }
            } catch (JavaModelException e) {
                throw ErrorUtil.newCoreException("addGWTLibraries failed", e);
            }
        } finally {
            monitor.done();
        }

    }

    /**
     * Remove GWT libraries from a project build path.
     * 
     * @param jproject
     * @param monitor
     * @throws CoreException
     */
    public static void removeGWTFromClasspath(IJavaProject jproject,
            IProgressMonitor monitor) throws CoreException {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        try {
            monitor.beginTask("Removing GWT libraries", 1);

            IClasspathEntry[] rawClasspath = jproject.getRawClasspath();
            List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
            for (IClasspathEntry entry : rawClasspath) {
                boolean add = true;
                // Skip GWT and dependencies, copy the rest
                IPath p = entry.getResolvedEntry().getPath();
                if ("gwt-dev.jar".equals(p.lastSegment())
                        || "gwt-user.jar".equals(p.lastSegment())
                        || ProjectUtil.isGWTDependency(jproject, p)) {
                    add = false;
                }
                if (add) {
                    entries.add(entry);
                }
            }

            IClasspathEntry[] entryArray = entries
                    .toArray(new IClasspathEntry[entries.size()]);
            jproject.setRawClasspath(entryArray, null);

            monitor.worked(1);

            // TODO update classpaths also in launches

        } catch (JavaModelException e) {
            throw ErrorUtil
                    .newCoreException("removeGWTFromClasspath failed", e);
        } finally {
            monitor.done();
        }

    }

    /**
     * Checks if the project is using a custom (user-defined) GWT version
     * directly on the build path.
     * 
     * @param jproject
     * @param useContainers
     *            also look into classpath containers such as WEB-INF/lib
     * @return true if the classpath contains GWT JARs other than those managed
     *         by the plugin
     */
    private static boolean isUsingUserDefinedGwt(IJavaProject jproject,
            boolean useContainers) {
        try {
            // make sure both kinds of paths are handled
            String gwtDownloadPath1 = VaadinPluginUtil.getDownloadDirectory()
                    .toPortableString();
            String gwtDownloadPath2 = VaadinPluginUtil.getDownloadDirectory()
                    .toOSString();

            IClasspathEntry[] rawClasspath = jproject.getRawClasspath();
            for (int i = 0; i < rawClasspath.length; i++) {
                IClasspathEntry cp = rawClasspath[i];
                if (cp.toString().contains("gwt-dev")
                        || cp.toString().contains("gwt-user")) {
                    if (!cp.toString().startsWith("VAADIN_DOWNLOAD")
                            && !cp.toString().startsWith(gwtDownloadPath1)
                            && !cp.toString().startsWith(gwtDownloadPath2)) {
                        return true;
                    }
                } else if (useContainers
                        && cp.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
                    // primarily WEB-INF/lib
                    IClasspathContainer container = JavaCore
                            .getClasspathContainer(cp.getPath(), jproject);
                    if (container == null) {
                        // Container is null at least for an invalid JRE
                        // reference
                        continue;
                    }

                    IClasspathEntry[] containerEntries = container
                            .getClasspathEntries();
                    for (IClasspathEntry ccp : containerEntries) {
                        if (ccp.toString().contains("gwt-dev")
                                || ccp.toString().contains("gwt-user")) {
                            return true;
                        }
                    }
                }
            }
        } catch (CoreException e) {
            ErrorUtil
                    .handleBackgroundException(
                            IStatus.WARNING,
                            "Could not determine whether the project uses user-defined GWT JARs. Assuming GWT JARs are managed by the plugin.",
                            e);
        }
        return false;
    }

    /**
     * Returns the first gwt dev jar defined in project classpath.
     * 
     * If not set, a gwt jar file provided by plugin is returned, or null if
     * none specified by Vaadin JAR.
     */
    public static IPath getGWTDevJarPath(IJavaProject jproject)
            throws CoreException {
        // check first for explicitly set gwt-dev jar file
        IClasspathEntry[] rawClasspath = jproject.getRawClasspath();
        for (IClasspathEntry cp : rawClasspath) {
            if (cp.toString().contains("gwt-dev")) {
                // User has explicitly defined GWT version to use directly on
                // the classpath, or classpath entry created by the plugin
                IClasspathEntry resolvedClasspathEntry = JavaCore
                        .getResolvedClasspathEntry(cp);
                return resolvedClasspathEntry.getPath();
            } else if (cp.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
                // primarily WEB-INF/lib
                IClasspathContainer container = JavaCore.getClasspathContainer(
                        cp.getPath(), jproject);
                if (container != null) {
                    IClasspathEntry[] containerEntries = container
                            .getClasspathEntries();
                    for (IClasspathEntry ccp : containerEntries) {
                        if (ccp.toString().contains("gwt-dev")) {
                            // User has explicitly defined GWT version to use
                            IClasspathEntry resolvedClasspathEntry = JavaCore
                                    .getResolvedClasspathEntry(ccp);
                            return resolvedClasspathEntry.getPath();
                        }
                    }
                }
            }
        }

        String gwtVersion = ProjectUtil
                .getRequiredGWTVersionForProject(jproject);
        if (gwtVersion == null) {
            return null;
        } else {
            return LocalFileManager.getLocalGwtDevJar(gwtVersion);
        }
    }

}
