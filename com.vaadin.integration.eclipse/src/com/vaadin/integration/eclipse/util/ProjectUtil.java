package com.vaadin.integration.eclipse.util;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jst.j2ee.web.componentcore.util.WebArtifactEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualFolder;

import com.vaadin.integration.eclipse.VaadinFacetUtils;
import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.util.files.LocalFileManager;

// TODO this class needs cleanup of all the project related methods - there is a lot of overlap etc.
public class ProjectUtil {
    private static final String DEFAULT_GWT_VERSION = "2.0.4";

    public static IProject getAnyProject(ISelection selection) {
        IResource resource = extractSelection(selection);
        if (resource != null) {
            return resource.getProject();
        }

        return null;
    }

    private static IResource extractSelection(ISelection sel) {
        if (!(sel instanceof IStructuredSelection) || sel.isEmpty()) {
            return null;
        }
        IStructuredSelection ss = (IStructuredSelection) sel;
        Object element = ss.getFirstElement();
        if (element instanceof IResource) {
            return (IResource) element;
        }
        if (!(element instanceof IAdaptable)) {
            return null;
        }
        IAdaptable adaptable = (IAdaptable) element;
        Object adapter = adaptable.getAdapter(IResource.class);
        return (IResource) adapter;
    }

    /**
     * Find a project based on a selection.
     * 
     * If the selection is an element in a suitable project, return that
     * project.
     * 
     * Note that this method can also return Java projects that do not have the
     * Vaadin facet!
     * 
     * Otherwise, return null.
     * 
     * @param selection
     * @return a project or null
     */
    public static IProject getProject(ISelection selection) {
        IProject project = null;
        Object obj = getSingleSelection(selection);
        if (obj != null) {
            IStructuredSelection ssel = (IStructuredSelection) selection;
            if (ssel instanceof TreeSelection) {
                TreeSelection ts = (TreeSelection) ssel;
                obj = ts.getPaths()[0].getFirstSegment();
            }
            if (obj instanceof IJavaProject) {
                return ((IJavaProject) obj).getProject();
            }
            if (obj instanceof IResource) {
                project = getProject((IResource) obj);
            } else if (obj instanceof IJavaProject) {
                project = ((IJavaProject) obj).getProject();
            }
        }
        return project;
    }

    /**
     * Find a project based on a resource.
     * 
     * If the resource is an element in a suitable project, return that project.
     * 
     * Otherwise, return null.
     * 
     * @param resource
     *            a file or some other resource, can be null
     * @return a project or null
     */
    public static IProject getProject(IResource resource) {
        IContainer container = null;
        IProject project = null;
        if (resource instanceof IContainer) {
            container = (IContainer) resource;
        } else if (resource != null) {
            container = (resource).getParent();
        }
        if (container != null) {
            project = container.getProject();
        }
        return project;
    }

    /**
     * Returns the file open in an active editor.
     * 
     * @param editor
     *            active editor, can be null
     * @return file being edited or null
     */
    public static IFile getFileForEditor(IEditorPart editor) {
        IFile file = null;
        if (editor != null
                && editor.getEditorInput() instanceof IFileEditorInput) {
            IFileEditorInput input = (IFileEditorInput) editor.getEditorInput();
            file = input.getFile();
        }
        return file;
    }

    /**
     * Find a project based on current selection and active editor.
     * 
     * If the current selection is not a single element structured selection,
     * the open file in the editor is used to find the relevant project.
     * 
     * @param currentSelection
     *            current selection, can be null
     * @param activeEditor
     *            currently active editor, can be null
     * @return project or null if no suitable project found based on selection
     *         and active editor
     */
    public static IProject getProject(ISelection currentSelection,
            IEditorPart activeEditor) {
        Object obj = getSingleSelection(currentSelection);
        if (obj != null) {
            if (obj instanceof IFile) {
                IFile file = (IFile) obj;
                return file.getProject();
            }
            IProject project = ProjectUtil.getProject(currentSelection);
            if (project == null) {
                IFile file = getFileForEditor(activeEditor);
                if (file != null && file.exists()) {
                    return file.getProject();
                }
            } else {
                return project;
            }
        } else {
            IFile file = getFileForEditor(activeEditor);
            if (file != null) {
                return file.getProject();
            }
        }
        return null;
    }

    /**
     * Returns the selection content if a structured selection with size one,
     * otherwise return null.
     * 
     * @param currentSelection
     *            current selection, can be null
     * @return single selected item or null if none or multiple selection
     */
    public static Object getSingleSelection(ISelection currentSelection) {
        if (currentSelection instanceof IStructuredSelection
                && ((IStructuredSelection) currentSelection).size() == 1) {
            IStructuredSelection ssel = (IStructuredSelection) currentSelection;
            return ssel.getFirstElement();
        }
        return null;
    }

    public static IFolder getWebInfLibFolder(IProject project)
            throws CoreException {
        IVirtualComponent component = ComponentCore.createComponent(project);
        if (component == null) {
            throw ErrorUtil
                    .newCoreException("Unable to find WEB-INF/lib folder. Ensure the project is a dynamic web project.");
        }
        IVirtualFolder contentFolder = component.getRootFolder();
        return (IFolder) contentFolder.getFolder(WebArtifactEdit.WEBLIB)
                .getUnderlyingFolder();
    }

    public static IFolder getWebInfFolder(IProject project)
            throws CoreException {
        IVirtualComponent component = ComponentCore.createComponent(project);
        if (component == null) {
            throw ErrorUtil
                    .newCoreException("Unable to locate WEB-INF folder. Ensure the project is a dynamic web project.");
        }
        IVirtualFolder contentFolder = component.getRootFolder();
        IContainer underlying = contentFolder
                .getFolder(WebArtifactEdit.WEB_INF).getUnderlyingFolder();
        if (!(underlying instanceof IFolder)) {
            throw ErrorUtil
                    .newCoreException("Unable to locate WEB-INF folder. Ensure the project is a dynamic web project.");
        }
        return (IFolder) underlying;
    }

    public static IFolder getWebContentFolder(IProject project)
            throws CoreException {
        IVirtualComponent component = ComponentCore.createComponent(project);
        if (component == null) {
            throw ErrorUtil
                    .newCoreException("Unable to locate WebContent folder. Ensure the project is a dynamic web project.");
        }
        IVirtualFolder contentFolder = component.getRootFolder();
        IContainer underlying = contentFolder.getUnderlyingFolder();
        if (!(underlying instanceof IFolder)) {
            throw ErrorUtil
                    .newCoreException("Unable to locate WebContent folder. Ensure the project is a dynamic web project.");
        }
        return (IFolder) underlying;
    }

    public static IFolder getThemesFolder(IProject project)
            throws CoreException {
        String VAADIN = VaadinPlugin.VAADIN_RESOURCE_DIRECTORY;
        String themes = VaadinPlugin.THEME_FOLDER_NAME;
        return ProjectUtil.getWebContentFolder(project.getProject())
                .getFolder(VAADIN).getFolder(themes);
    }

    public static IFolder getAddonsFolder(IProject project)
            throws CoreException {
        String VAADIN = VaadinPlugin.VAADIN_RESOURCE_DIRECTORY;
        String addons = VaadinPlugin.VAADIN_ADDON_THEME_DIRECTORY;
        return ProjectUtil.getWebContentFolder(project.getProject())
                .getFolder(VAADIN).getFolder(addons);
    }

    public static IFolder getSrcFolder(IProject project) throws CoreException {
        try {
            IJavaProject javaProject = JavaCore.create(project);
            for (IClasspathEntry classPathEntry : javaProject.getRawClasspath()) {
                if (classPathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                    IPath path = classPathEntry.getPath();
                    return project.getWorkspace().getRoot().getFolder(path);
                    // return project.getFolder(path);
                }
            }

            return null;
        } catch (JavaModelException e) {
            throw ErrorUtil.newCoreException("Unable to locate source folder",
                    e);
        }
    }

    /**
     * Find the Vaadin Application type for a project or null if none found.
     * 
     * @param jproject
     * @return
     * @throws JavaModelException
     */
    public static IType findVaadinApplicationType(IJavaProject jproject)
            throws JavaModelException {
        if (jproject == null) {
            return null;
        }
        return jproject.findType(VaadinPlugin.APPLICATION_CLASS_FULL_NAME);
    }

    /**
     * Find the Vaadin UI type for a project or null if none found.
     * 
     * @param jproject
     * @return
     * @throws JavaModelException
     */
    public static IType findVaadinUiType(IJavaProject jproject)
            throws JavaModelException {
        if (jproject == null) {
            return null;
        }
        return jproject.findType(VaadinPlugin.UI_CLASS_FULL_NAME);
    }

    /**
     * Returns a list of jar files required by the GWT version used by the
     * Vaadin jar in the project.
     * 
     * @param jproject
     * @return
     */
    public static List<String> getRequiredGWTDependenciesForProject(
            IJavaProject jproject) {
        try {
            // find Vaadin JAR on the classpath
            IPath vaadinJarPath = ProjectUtil
                    .findProjectVaadinJarPath(jproject);
            return getRequiredGWTDependenciesForVaadinJar(vaadinJarPath);

        } catch (CoreException e) {
            ErrorUtil.handleBackgroundException(IStatus.WARNING,
                    "Failed to determine required GWT dependencies.", e);
        }

        return new ArrayList<String>();
    }

    /**
     * Returns a list of jar files required by the GWT version used by a
     * specified Vaadin jar.
     * 
     * @param vaadinJarPath
     * @return
     */
    public static List<String> getRequiredGWTDependenciesForVaadinJar(
            IPath vaadinJarPath) {
        try {
            if (vaadinJarPath != null) {
                return VersionUtil
                        .getRequiredGWTDependenciesForVaadinJar(vaadinJarPath);
            }

        } catch (IOException e) {
            ErrorUtil.handleBackgroundException(IStatus.WARNING,
                    "Failed to determine required GWT dependencies.", e);
        }

        return new ArrayList<String>();
    }

    public static String getRequiredGWTVersionForProject(IJavaProject jproject) {
        try {
            // find Vaadin JAR on the classpath
            IPath vaadinJarPath = ProjectUtil
                    .findProjectVaadinJarPath(jproject);

            return getRequiredGWTVersionForVaadinJar(vaadinJarPath);

        } catch (CoreException ex) {
            ErrorUtil.handleBackgroundException(IStatus.WARNING,
                    "Failed to determine the GWT library version to use.", ex);
        }

        // did not find Vaadin JAR, default to 2.0.4
        return DEFAULT_GWT_VERSION;
    }

    public static String getRequiredGWTVersionForVaadinJar(IPath vaadinJarPath) {
        try {
            // find Vaadin JAR on the classpath
            if (vaadinJarPath != null) {
                String gwtVersion = VersionUtil
                        .getRequiredGWTVersionForVaadinJar(vaadinJarPath);
                return gwtVersion;
            }
        } catch (IOException ex) {
            ErrorUtil.handleBackgroundException(IStatus.WARNING,
                    "Failed to determine the GWT library version to use.", ex);
        }
        // if no information exists, default to none
        return null;
    }

    public static boolean isGwt20(IProject project) {
        IJavaProject jproject = JavaCore.create(project);
        if (jproject != null) {
            try {
                // with Vaadin 7, DevMode might not be on the project classpath
                // but there isGwt24() should return true
                if (jproject.findType("com.google.gwt.dev.DevMode") != null
                        || isGwt24(project)) {
                    return true;
                }
            } catch (JavaModelException e) {
                ErrorUtil
                        .handleBackgroundException(
                                IStatus.WARNING,
                                "Failed to check the GWT version used in the project, assuming 1.x",
                                e);
            }
        }
        // default value
        return false;
    }

    public static boolean isGwt24(IProject project) {
        IJavaProject jproject = JavaCore.create(project);
        if (jproject != null) {
            try {
                if (jproject
                        .findType("com.google.gwt.geolocation.client.Position") != null) {
                    return true;
                }
            } catch (JavaModelException e) {
                ErrorUtil
                        .handleBackgroundException(
                                IStatus.WARNING,
                                "Failed to check the GWT version used in the project, assuming 2.3 or earlier",
                                e);
            }
        }
        // default value
        return false;
    }

    /**
     * Checks which Vaadin version is in use in the project. This uses
     * {@link #getVaadinLibraryInProject(IProject, boolean)} to find the Vaadin
     * jar and gets the version from the metadata in the jar.
     * 
     * @param project
     *            project to check, can be null
     * @param useClasspath
     *            true to also search the classpath if no Vaadin JAR is found in
     *            WEB-INF/lib
     * @return The version of the Vaadin JAR in the project. Returns null if no
     *         Vaadin JAR was found or if the version number could not be
     *         determined.
     * @throws CoreException
     */
    public static String getVaadinLibraryVersion(IProject project,
            boolean useClasspath) throws CoreException {
        IPath vaadinLibrary = getVaadinLibraryInProject(project, useClasspath);
        if (vaadinLibrary != null) {
            return VersionUtil.getVaadinVersionFromJar(vaadinLibrary);
        }

        return null;
    }

    /**
     * Locates the Vaadin JAR in the project. Searches WEB-INF/lib for a Vaadin
     * JAR and optionally (useClasspath parameter) also the full classpath.
     * 
     * A file is considered a Vaadin JAR if it contains the correct metadata
     * 
     * @param project
     * @param useClasspath
     *            true to also search elsewhere on the classpath if no Vaadin
     *            JAR is found in WEB-INF/lib
     * @return The version of the Vaadin JAR currently in the project. Returns
     *         null if no Vaadin JAR was found or if the version number could
     *         not be determined.
     * @throws CoreException
     */
    public static IPath getVaadinLibraryInProject(IProject project,
            boolean useClasspath) throws CoreException {
        IFolder lib = ProjectUtil.getWebInfLibFolder(project);
        if (lib != null && lib.exists()) {
            IResource[] files = lib.members();
            for (IResource resource : files) {
                // is it a Vaadin JAR?
                if (resource instanceof IFile) {
                    if (VersionUtil
                            .couldBeOfficialVaadinJar(resource.getName())) {
                        // Name matches vaadin jar, still check for version from
                        // the jar itself

                        String version = VersionUtil
                                .getVaadinVersionFromJar(resource.getFullPath());

                        if (version != null) {
                            return resource.getFullPath();
                        }
                    }
                }
            }
        }

        if (useClasspath) {
            IJavaProject jproject = JavaCore.create(project);
            IPath resource = ProjectUtil.findProjectVaadinJarPath(jproject);
            String version = VersionUtil.getVaadinVersionFromJar(resource);
            if (version != null) {
                return resource;
            }
        }

        return null;
    }

    public static IPath findProjectVaadinJarPath(IJavaProject javaProject)
            throws CoreException {
        IJavaElement type;
        type = ProjectUtil.findVaadinApplicationType(javaProject);
        if (null == type) {
            // try UI if Vaadin 7
            type = ProjectUtil.findVaadinUiType(javaProject);
        }

        while (type != null && type.getParent() != null) {
            if (type instanceof IPackageFragmentRoot) {
                IPackageFragmentRoot jar = (IPackageFragmentRoot) type;
                IResource resource = jar.getResource();
                if (resource == null) {
                    // Galileo
                    return jar.getPath();
                } else {
                    // Ganymede
                    IPath rawLocation = resource.getRawLocation();
                    return rawLocation;
                }
            }
            type = type.getParent();
        }

        // at project creation, maybe not yet compiled => search for JARs on
        // the classpath
        IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
        for (IClasspathEntry cp : rawClasspath) {
            if (cp.toString().contains(".jar")) {
                // User has explicitly defined GWT version to use directly on
                // the classpath, or classpath entry created by the plugin
                IClasspathEntry resolvedClasspathEntry = JavaCore
                        .getResolvedClasspathEntry(cp);
                IPath path = resolvedClasspathEntry.getPath();
                if (VaadinPluginUtil.isVaadinJar(path)) {
                    return path;
                }
            } else if (cp.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
                // primarily WEB-INF/lib, but possibly also Liferay etc.
                IClasspathContainer container = JavaCore.getClasspathContainer(
                        cp.getPath(), javaProject);
                if (container == null) {
                    // Container is null at least for an invalid JRE reference
                    continue;
                }
                IClasspathEntry[] containerEntries = container
                        .getClasspathEntries();
                for (IClasspathEntry ccp : containerEntries) {
                    if (ccp.toString().contains(".jar")) {
                        // User has explicitly defined GWT version to use
                        IClasspathEntry resolvedClasspathEntry = JavaCore
                                .getResolvedClasspathEntry(ccp);
                        IPath path = resolvedClasspathEntry.getPath();
                        if (VaadinPluginUtil.isVaadinJar(path)) {
                            return path;
                        }
                    }
                }
            }
        }

        // still no luck? check WEB-INF/lib
        IFolder lib = ProjectUtil.getWebInfLibFolder(javaProject.getProject());
        if (lib == null || !lib.exists()) {
            return null;
        }
        for (IResource resource : lib.members()) {
            // is it a Vaadin JAR?
            if (resource instanceof IFile
                    && VaadinPluginUtil.isVaadinJar(resource.getLocation())) {
                return resource.getLocation();
            }
        }

        // For some reason we were not able to locate the Vaadin JAR
        return null;
    }

    /**
     * Check is a project uses Vaadin 6.2 or later.
     * 
     * @param project
     * @return
     */
    /*-
    public static boolean isVaadin62(IProject project) {
        IPath findProjectVaadinJarPath;
        try {
            findProjectVaadinJarPath = ProjectUtil
                    .findProjectVaadinJarPath(JavaCore.create(project));
        } catch (CoreException e) {
            return false;
        }

        return findProjectVaadinJarPath != null
                && WidgetsetUtil.isWidgetsetPackage(findProjectVaadinJarPath);

    }
    -*/

    /**
     * Check that the project has Vaadin 7 or later on its classpath.
     * 
     * Effectively this method checks for the existence of the class
     * com.vaadin.ui.UI on the classpath.
     * 
     * @param project
     *            project to check
     * @return true if the project uses Vaadin 7 or later
     */
    public static boolean isVaadin7(IProject project) {
        try {
            // String vaadinVersion = getVaadinLibraryVersion(project, true);
            // return VersionUtil.isVaadin7VersionString(vaadinVersion);
            IType uiType = findVaadinUiType(JavaCore.create(project));
            return (null != uiType);
        } catch (CoreException e) {
            // ErrorUtil.handleBackgroundException("", e);
            return false;
        }
    }

    public static boolean isVaadin71(IProject project) {
        try {
            IJavaProject jproject = JavaCore.create(project);
            if (null == jproject) {
                return false;
            }
            IType type = jproject
                    .findType(VaadinPlugin.VAADIN_SERVLET_CONFIGURATION_ANNOTATION_FULL_NAME);
            return (null != type);
        } catch (CoreException e) {
            ErrorUtil.handleBackgroundException(
                    "Failed to check Vaadin version in project", e);
            return false;
        }
    }

    public static double getVaadinVersion(IProject project) {
        try {
            String vaadinVersion = getVaadinLibraryVersion(project, true);

            if (vaadinVersion == null) {
                // Yeah... Probably 6.0, but...
                return Double.MIN_VALUE;
            }

            String versionString = vaadinVersion.substring(0,
                    vaadinVersion.indexOf(".", vaadinVersion.indexOf(".") + 1));

            return Double.parseDouble(versionString);

            // TODO indicate to user if this fails -> unknown version
        } catch (CoreException e) {
            ErrorUtil.handleBackgroundException("", e);
            return Double.MAX_VALUE;
        } catch (NumberFormatException e) {
            ErrorUtil.handleBackgroundException("", e);
            return Double.MAX_VALUE;
        }
    }

    /**
     * Ensure the project uses the latest Vaadin facet if the project is a
     * faceted project using Vaadin. Also ensure that the project has the
     * widgetset nature if the widgetset is managed by the plugin.
     * 
     * @param project
     *            The target project
     */
    public static void ensureVaadinFacetAndNature(IProject project) {
        VaadinFacetUtils.fixFacetVersion(project);
        if (WidgetsetUtil.isWidgetsetManagedByPlugin(project)) {
            WidgetsetUtil.ensureWidgetsetNature(project);
        }

    }

    /**
     * Checks if the given path is inside the root folder of the given project.
     * 
     * @param project
     * @param vaadinLibrary
     * @return
     * @throws CoreException
     */
    public static boolean isInProject(IProject project, IPath path)
            throws CoreException {
        IPath root = project.getLocation();
        if (root.isPrefixOf(path)) {
            return true;
        }

        return false;
    }

    public static boolean isGWTDependency(IJavaProject jproject, IPath path)
            throws CoreException {
        return LocalFileManager.isGWTDependency(path);
    }

    public static boolean hasManifestAttribute(String attribute, IPath resource) {
        if (resource != null && resource.toPortableString().endsWith(".jar")) {
            JarFile jarFile = null;
            try {
                URL url = new URL("file:" + resource.toPortableString());
                url = new URL("jar:" + url.toExternalForm() + "!/");
                JarURLConnection conn = (JarURLConnection) url.openConnection();
                jarFile = conn.getJarFile();
                Manifest manifest = jarFile.getManifest();
                VaadinPluginUtil.closeJarFile(jarFile);
                jarFile = null;
                Attributes mainAttributes = manifest.getMainAttributes();
                if (mainAttributes.getValue(attribute) != null) {
                    return true;
                }
            } catch (Throwable t) {
                ErrorUtil.handleBackgroundException(IStatus.INFO,
                        "Could not access JAR when checking for attribute "
                                + attribute, t);
            } finally {
                VaadinPluginUtil.closeJarFile(jarFile);
            }
        }
        return false;
    }

}
