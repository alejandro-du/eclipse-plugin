package com.vaadin.integration.eclipse.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.text.Document;
import org.eclipse.jst.j2ee.web.componentcore.util.WebArtifactEdit;
import org.eclipse.jst.j2ee.webapplication.WebAppResource;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.console.MessageConsoleStream;

import com.vaadin.integration.eclipse.VaadinFacetUtils;
import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.builder.WidgetsetBuildManager;
import com.vaadin.integration.eclipse.builder.WidgetsetNature;
import com.vaadin.integration.eclipse.consoles.CompileWidgetsetConsole;
import com.vaadin.integration.eclipse.maven.MavenUtil;
import com.vaadin.integration.eclipse.wizards.DirectoryManifestProvider;

public class WidgetsetUtil {

    private static final String[] PROXY_PROPERTIES = {"http.proxyHost","http.proxyPort", "http.nonProxyHosts",
    		"https.proxyHost","https.proxyPort", "socksProxyHost","socksProxyPort" };
	private static final String DEFAULT_WIDGET_SET_NAME_VAADIN6 = "com.vaadin.terminal.gwt.DefaultWidgetSet";
    public static final String DEFAULT_WIDGET_SET_NAME = "com.vaadin.DefaultWidgetSet";

    /**
     * Helper method to compile a single widgetset.
     *
     * Instead the "old method" of using launch configurations (.launch) running
     * compilation via {@link ProcessBuilder}. Also notifies eclipse of possibly
     * changed files in widgetset directory.
     *
     * Note, this only works for projects with vaadin 6.2 and later.
     *
     * Normally this method should be called by {@link WidgetsetBuildManager} to
     * ensure that multiple builds of the same widgetset are not run
     * concurrently.
     *
     * @param jproject
     * @param moduleName
     *            explicit widgetset module name - not null
     * @param monitor
     * @throws CoreException
     * @throws IOException
     * @throws InterruptedException
     */
    public static void compileWidgetset(IJavaProject jproject,
            String moduleName, final IProgressMonitor monitor)
                    throws CoreException, IOException, InterruptedException {

        IProject project = jproject.getProject();

        if (!isWidgetsetManagedByPlugin(project)) {
            return;
        }

        final long start = new Date().getTime();
        CompileWidgetsetConsole console = CompileWidgetsetConsole.get();

        try {
            PreferenceUtil preferences = PreferenceUtil.get(project);
            boolean verbose = preferences.isWidgetsetCompilationVerboseMode();

            final long estimatedCompilationTime = preferences
                    .getEstimatedCompilationTime();

            // TODO should report progress more correctly - unknown?
            monitor.beginTask("Compiling widgetset " + moduleName
                    + " in project " + project.getName(), 100 + 10 + 4);

            monitor.subTask("Checking GWT version in the project "
                    + project.getName());
            // make sure the project has GWT JARs
            ProjectDependencyManager.ensureGWTLibraries(project,
                    new SubProgressMonitor(monitor, 10));

            monitor.subTask("Preparing to compile widgetset " + moduleName
                    + " in project " + project.getName());

            IVMInstall vmInstall = VaadinPluginUtil.getJvmInstall(jproject,
                    true);
            if (!VaadinPluginUtil.isJdk16(vmInstall)
                    && ProjectUtil.isGwt24(project)) {
                ErrorUtil
                .displayWarningFromBackgroundThread(
                        "Java6 required",
                        "Widget set compilation requires Java6.\n"
                                + "The project can still use Java5 but you need to make JDK 6 available in Eclipse\n"
                                + "(see Preferences => Java => Installed JREs).");
            }

            boolean useNewGwtCompiler = jproject
                    .findType(VaadinPlugin.GWT_OLD_COMPILER_CLASS) == null;

            // refresh only WebContent/VAADIN/widgetsets
            final IFolder wsDir = ProjectUtil.getWebContentFolder(project)
                    .getFolder(VaadinPlugin.VAADIN_RESOURCE_DIRECTORY)
                    .getFolder("widgetsets");

            File tempDir = null;
            File baseDir = new File(System.getProperty("java.io.tmpdir"));
            tempDir = new File(baseDir, "widgetset_" + moduleName
                    + UUID.randomUUID().toString());

            // refresh this requires that the directory exists
            VaadinPluginUtil.createFolders(wsDir, new SubProgressMonitor(
                    monitor, 1));

            ArrayList<String> args = buildCommonArgs(jproject, vmInstall);

            if (useNewGwtCompiler) {
                args.add("-Dgwt.persistentunitcachedir=" + tempDir);
            }

            String extraJvmParams = preferences
                    .getWidgetsetCompilationExtraJvmParameters();
            if (extraJvmParams != null) {
                String[] params = extraJvmParams.split(" ");
                for (String param : params) {
                    if (!"".equals(param)) {
                        args.add(param);
                    }
                }
            }
            
			for (String propertyName: PROXY_PROPERTIES)
            {
	            String propertyValue = System.getProperty(propertyName);
	            if(propertyValue != null) {
	            	 args.add("-D" + propertyName + "=" + propertyValue);
	            }
            }
            // TODO run com.vaadin.terminal.gwt.widgetsetutils.WidgetSetBuilder
            // and com.google.gwt.dev.Compiler separately and directly if Java
            // 6, do not use WidgetsetCompiler in that case

            List<String> compilerArgs = new ArrayList<String>(args);
            String compilerClass = "com.vaadin.tools.WidgetsetCompiler";
            compilerArgs.add(compilerClass);

            if (useNewGwtCompiler) {
                compilerArgs.add("-war");
                IPath projectRelativePath = wsDir.getProjectRelativePath();
                compilerArgs.add(projectRelativePath.toString());

                if (!tempDir.mkdirs()) {
                    throw ErrorUtil
                    .newCoreException("Could not create temporary directory "
                            + tempDir);
                }

                compilerArgs.add("-deploy");
                compilerArgs.add(tempDir.getAbsolutePath());

                compilerArgs.add("-extra");
                compilerArgs.add(tempDir.getAbsolutePath());
            } else {
                compilerArgs.add("-out");
                IPath projectRelativePath = wsDir.getProjectRelativePath();
                compilerArgs.add(projectRelativePath.toString());
            }

            String style = preferences.getWidgetsetCompilationStyle();
            if ("DRAFT".equals(style)) {
                compilerArgs.add("-style");
                compilerArgs.add("PRETTY");
                compilerArgs.add("-draftCompile");
            } else if (!"OBF".equals(style)) {
                compilerArgs.add("-style");
                compilerArgs.add(style);
            }

            String parallelism = preferences
                    .getWidgetsetCompilationParallelism();
            if ("".equals(parallelism)) {
                compilerArgs.add("-localWorkers");
                compilerArgs.add(""
                        + Runtime.getRuntime().availableProcessors());
            } else {
                compilerArgs.add("-localWorkers");
                compilerArgs.add(parallelism);
            }

            if (verbose) {
                compilerArgs.add("-logLevel");
                compilerArgs.add("INFO");
            } else {
                compilerArgs.add("-logLevel");
                compilerArgs.add("WARN");
            }

            String extraParams = preferences
                    .getWidgetsetCompilationExtraParameters();
            if (extraParams != null) {
                String[] params = extraParams.split(" ");
                for (String param : params) {
                    if (!"".equals(param)) {
                        compilerArgs.add(param);
                    }
                }
            }

            compilerArgs.add(moduleName);

            final String[] argsStr = new String[compilerArgs.size()];
            compilerArgs.toArray(argsStr);

            ProcessBuilder b = new ProcessBuilder(argsStr);

            IPath projectLocation = project.getLocation();
            b.directory(projectLocation.toFile());

            b.redirectErrorStream(true);

            monitor.subTask("Compiling widgetset " + moduleName
                    + " in project " + project.getName());

            final Process exec = b.start();

            // compilation now on

            Thread t = new Thread() {
                @Override
                public synchronized void run() {
                    int i = 0;
                    int estimatedProgress = 0;

                    while (true) {
                        if (monitor.isCanceled()) {
                            exec.destroy();
                            break;
                        } else {
                            try {
                                i++;
                                // give user a feeling that something is
                                // happening

                                int currentProgress = (int) (100 * (new Date()
                                .getTime() - start) / estimatedCompilationTime);
                                if (currentProgress > 100) {
                                    currentProgress = 100;
                                }
                                int delta = currentProgress - estimatedProgress;

                                monitor.worked(delta);
                                estimatedProgress += delta;
                                Thread.sleep(300);
                            } catch (InterruptedException e) {
                                // STOP executing monitoring cancelled state,
                                // compilation finished
                                break;
                            }
                        }
                    }
                }
            };

            t.start();

            console.setCompilationProcess(exec);
            console.clearConsole();

            MessageConsoleStream newMessageStream = console.newMessageStream();

            console.activate();
            newMessageStream.println();
            if (verbose) {
                newMessageStream
                .println("Executing compiler with command line:");
                StringBuilder commandLine = new StringBuilder();
                for (String arg : compilerArgs) {
                    commandLine.append(" ").append(arg);
                }
                newMessageStream.println(commandLine.toString());
            } else {
                newMessageStream.println("Compiling widgetset " + moduleName);
            }

            // print warning if not using project VM (#8037)
            if (!vmInstall.equals(JavaRuntime.getVMInstall(jproject))) {
                newMessageStream
                .println("Warning: Not using project VM for GWT compilation.\n"
                        + "When using GWT 2.4, select JRE 1.6 or later in project preferences.");
            }

            InputStream inputStream = exec.getInputStream();
            BufferedReader bufferedReader2 = new BufferedReader(
                    new InputStreamReader(inputStream));
            String line = null;
            while ((line = bufferedReader2.readLine()) != null) {
                newMessageStream.println(line);
            }

            int waitFor = exec.waitFor();

            // end thread (possibly still) polling for cancelled status
            t.interrupt();

            if (waitFor == 0) {
                // Remove the widgetset-aux and widgetset-deploy dirs if they
                // were created (#4443 and #7099)
                // Must apparently refresh before so it is in the local
                // workspace, otherwise it won't exist and won't be deleted
                IFolder auxDir = wsDir.getFolder(moduleName + "-aux");
                auxDir.refreshLocal(0, new SubProgressMonitor(monitor, 1));
                auxDir.delete(true, null);

                IFolder deployDir = wsDir.getFolder(moduleName + "-deploy");
                deployDir.refreshLocal(0, new SubProgressMonitor(monitor, 1));
                deployDir.delete(true, null);

                if (tempDir != null && tempDir.exists()) {
                    deleteFolder(tempDir);
                }

                // Refresh the workspace so the new widgetset is visible
                wsDir.refreshLocal(IResource.DEPTH_INFINITE,
                        new SubProgressMonitor(monitor, 1));
                setWidgetsetDirty(project, false);
                preferences.setWidgetsetCompilationTimeEstimate(new Date()
                .getTime() - start);
                preferences.persist();

                if (!verbose) {
                    // if verbose, the output of the compiler is sufficient
                    newMessageStream.println("Widgetset compilation completed");
                }
            } else {
                // cancelled or failed
                setWidgetsetDirty(project, true);

                if (monitor.isCanceled()) {
                    newMessageStream.println("Widgetset compilation canceled");
                } else {
                    newMessageStream.println("Widgetset compilation failed");
                }
            }
        } finally {
            monitor.done();
            console.setCompilationProcess(null);

        }
    }

    public static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        // some JVMs return null for empty dirs
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }

    public static ArrayList<String> buildCommonArgs(IJavaProject jproject,
            IVMInstall vmInstall) throws CoreException, JavaModelException {
        ArrayList<String> args = new ArrayList<String>();

        String vmName = VaadinPluginUtil.getJvmExecutablePath(vmInstall);
        args.add(vmName);

        // construct the class path, including GWT JARs and project sources
        String classPath = VaadinPluginUtil.getProjectBaseClasspath(jproject,
                vmInstall, true);

        String classpathSeparator = PlatformUtil.getClasspathSeparator();

        // add widgetset JARs
        Collection<IPath> widgetpackages = getAvailableVaadinWidgetsetPackages(jproject);
        for (IPath file2 : widgetpackages) {
            // this filters out Vaadin JAR(s) already added to the classpath
            String jarName = file2.toString();
            if (!classPath.contains(jarName)) {
                classPath = classPath + classpathSeparator + jarName;
            }
        }

        // construct rest of the arguments for the launch

        args.add("-Djava.awt.headless=true");
        args.add("-Xss8M");
        args.add("-Xmx512M");
        if (!VaadinPluginUtil.isJdk18(vmInstall)) {
            args.add("-XX:MaxPermSize=512M");
        }

        args.add("-classpath");
        // args.add(classPath.replaceAll(" ", "\\ "));
        args.add(classPath);
        return args;
    }

    /**
     * Find the (first) widget set in the project. If there is none, return the
     * default widget set
     *
     * @param project
     * @return the first widget set GWT module name in the project or the
     *         default widget set
     */
    public static String getFirstWidgetSet(IJavaProject project) {
        try {
            List<String> widgetsets = WidgetsetUtil.findWidgetSets(project,
                    new NullProgressMonitor());
            for (String widgetset : widgetsets) {
                if (widgetset != null) {
                    return widgetset;
                }
            }
        } catch (CoreException e) {
            ErrorUtil.handleBackgroundException(
                    "Failed to find widgetset in the project", e);
        }

        if (!ProjectUtil.isVaadin7(project.getProject())) {
            return DEFAULT_WIDGET_SET_NAME_VAADIN6;
        }

        return DEFAULT_WIDGET_SET_NAME;
    }

    /**
     * Find the list of widgetsets in the project.
     *
     * Only GWT modules (.gwt.xml files) with "widgetset" in the file name are
     * returned.
     *
     * @param project
     * @param monitor
     * @return list of widgetset module names in the project
     * @throws CoreException
     */
    public static List<String> findWidgetSets(IJavaProject project,
            IProgressMonitor monitor) throws CoreException {
        IPackageFragmentRoot[] packageFragmentRoots = project
                .getPackageFragmentRoots();
        final List<IPath> paths = new ArrayList<IPath>();
        IResourceVisitor visitor = new IResourceVisitor() {
            public boolean visit(IResource arg0) throws CoreException {
                if (arg0 instanceof IFile) {
                    IFile f = (IFile) arg0;
                    String name = f.getName();
                    if (name.endsWith(".gwt.xml")
                            && name.toLowerCase().contains("widgetset")) {
                        paths.add(f.getFullPath());
                        return false;
                    }
                }
                return true;
            }
        };

        List<String> widgetsets = new ArrayList<String>();
        for (int i = 0; i < packageFragmentRoots.length; i++) {
            if (!packageFragmentRoots[i].isArchive()) {
                IResource underlyingResource = packageFragmentRoots[i]
                        .getUnderlyingResource();
                if (underlyingResource != null) {
                    underlyingResource.accept(visitor);

                    for (IPath path : paths) {
                        String wspath = path.toString();
                        IPath fullPath = underlyingResource.getFullPath();
                        wspath = wspath.replace(fullPath.toString() + "/", "");
                        wspath = wspath.replaceAll("/", ".").replaceAll(
                                ".gwt.xml", "");
                        widgetsets.add(wspath);
                    }

                    paths.clear();
                }
            }
        }

        return widgetsets;
    }

    /**
     * Check if a project contains one or more widgetsets. This method is more
     * efficient than {@link #findWidgetSets(IJavaProject, IProgressMonitor)} as
     * the evaluation stops upon the first match.
     *
     * Only GWT modules (.gwt.xml files) with "widgetset" in the file name are
     * taken into account.
     *
     * @param project
     * @param monitor
     * @return true if the project directly contains at least one widgetset
     * @throws CoreException
     */
    public static boolean hasWidgetSets(IJavaProject project,
            IProgressMonitor monitor) throws CoreException {
        final boolean[] found = new boolean[] { false };
        IResourceVisitor visitor = new IResourceVisitor() {
            public boolean visit(IResource arg0) throws CoreException {
                if (found[0]) {
                    return false;
                }
                if (arg0 instanceof IFile) {
                    IFile f = (IFile) arg0;
                    String name = f.getName();
                    if (name.endsWith(".gwt.xml")
                            && name.toLowerCase().contains("widgetset")) {
                        found[0] = true;
                        return false;
                    }
                }
                return true;
            }
        };

        IPackageFragmentRoot[] packageFragmentRoots = project
                .getPackageFragmentRoots();
        for (int i = 0; i < packageFragmentRoots.length; i++) {
            if (!packageFragmentRoots[i].isArchive()) {
                IResource underlyingResource = packageFragmentRoots[i]
                        .getUnderlyingResource();
                underlyingResource.accept(visitor);

                if (found[0]) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Find the list of widgetsets in the project in a format suitable for a
     * Vaadin addon manifest file.
     *
     * @param project
     * @param monitor
     * @return String comma-separated list of widgetset module names in the
     *         project, not null
     * @throws CoreException
     */
    public static String findWidgetSetsString(IJavaProject project,
            IProgressMonitor monitor) throws CoreException {
        List<String> widgetsets = findWidgetSets(project, monitor);
        StringBuilder result = new StringBuilder();
        Iterator<String> it = widgetsets.iterator();
        while (it.hasNext()) {
            result.append(it.next());
            if (it.hasNext()) {
                result.append(",");
            }
        }
        return result.toString();
    }

    public static boolean isWidgetsetPackage(IPath resource) {
        return ProjectUtil.hasManifestAttribute("Vaadin-Widgetsets", resource);
    }

    private static boolean isNeededForWidgetsetCompilation(IPath path) {
        if ("jar".equals(path.getFileExtension())) {
            JarFile jarFile = null;
            try {
                URL url = path.toFile().toURL();
                url = new URL("jar:" + url.toExternalForm() + "!/");
                JarURLConnection conn = (JarURLConnection) url.openConnection();
                jarFile = conn.getJarFile();
                Manifest manifest = jarFile.getManifest();
                if (manifest == null) {
                    return false;
                }
                Attributes mainAttributes = manifest.getMainAttributes();
                if (mainAttributes
                        .getValue(DirectoryManifestProvider.MANIFEST_VAADIN_WIDGETSETS) != null) {
                    return true;
                } else {
                    // not a vaadin widget package, but it still may be
                    // needed for referenced gwt modules (cant know for
                    // sure)

                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry nextElement = entries.nextElement();
                        if (nextElement.getName().endsWith(".gwt.xml")) {
                            return true;
                        }
                    }
                }
            } catch (MalformedURLException e) {
                String message = (jarFile == null) ? "Could not access JAR when searching for widgetsets"
                        : "Could not access JAR when searching for widgetsets: "
                        + jarFile.getName();
                ErrorUtil
                .handleBackgroundException(IStatus.WARNING, message, e);
            } catch (IOException e) {
                String message = (jarFile == null) ? "Could not access JAR when searching for widgetsets"
                        : "Could not access JAR when searching for widgetsets: "
                        + jarFile.getName();
                ErrorUtil
                .handleBackgroundException(IStatus.WARNING, message, e);
            } finally {
                VaadinPluginUtil.closeJarFile(jarFile);
            }
        } else {
            // detect if is jar and if in widgetset
        }
        return false;
    }

    /**
     * Returns true if the widgetset should be managed (created, compiled etc.)
     * by the plugin.
     *
     * @param project
     *            the project to check, can be null
     * @return true if the widgetset should be automatically managed
     */
    public static boolean isWidgetsetManagedByPlugin(IProject project) {
        return project != null;
    }

    /**
     * Checks if the widgetset in a project is marked as dirty. If the project
     * is not a Vaadin project or does not have widgetsets, returns
     * <code>false</code>.
     *
     * If the flag is not present in project preferences, test whether there are
     * widgetsets and as a side effect mark dirty (if any exist) / clean (no
     * widgetset) based on that.
     *
     * @param project
     * @return true if the project has widgetset(s) that have not been compiled
     *         since the last relevant modification
     */
    public static boolean isWidgetsetDirty(IProject project) {
        // default to clean until some widgetset found
        boolean result = false;
        try {
            if (MavenUtil.isMavenProject(project)
                    || !VaadinFacetUtils.isVaadinProject(project)) {
                // widgetsets are handled differently for Maven projects
                return false;
            }
            IProgressMonitor monitor = new NullProgressMonitor();
            if (ProjectUtil.getVaadinVersion(project) < 6.2) {
                return false;
            }

            // from this point on, default to dirty
            result = true;

            PreferenceUtil preferences = PreferenceUtil.get(project);
            Boolean dirty = preferences.isWidgetsetDirty();
            if (dirty == null) {
                result = hasWidgetSets(JavaCore.create(project), monitor);
                setWidgetsetDirty(project, result);
                dirty = result;
            }
            return dirty;
        } catch (CoreException e) {
            return result;
        }
    }

    /**
     * Mark the widgetset(s) in a project as clean (compiled) or dirty (modified
     * since the last compilation).
     *
     * TODO note: keeping track of this in preferences might be an issue with
     * version control etc. if versioning preferences
     *
     * @param project
     * @param dirty
     */
    public static void setWidgetsetDirty(IProject project, boolean dirty) {
        // save as string so that the value false does not result in the entry
        // being removed - we use three states: true, false and absent
        PreferenceUtil preferences = PreferenceUtil.get(project);
        preferences.setWidgetsetDirty(dirty);
        try {
            preferences.persist();
        } catch (IOException e) {
            ErrorUtil.handleBackgroundException(IStatus.WARNING,
                    "Could not save widgetset compilation state for project "
                            + project.getName(), e);
        }

    }

    /**
     * Find the (first) widgetset in a project, or indicate where the widgetset
     * should be created. If <code>create</code> is true, create the widgetset
     * if it did not exist.
     *
     * Unless explicitly given, the default location for a new widgetset is
     * based on the location of the Application class with the shortest package
     * path. A "widgetset" package is created under that package.
     *
     * A widgetset file should be named *widgetset*.gwt.xml - the ".gwt.xml" is
     * not a part of the module name.
     *
     * @param project
     * @param create
     *            create widgetset if it does not exist
     * @param root
     *            package fragment root under which the widgetset should be
     *            created, null for default/automatic
     * @param defaultPackage
     *            package name under which to create the widgetset, null to
     *            deduce from application locations - default (empty) package is
     *            not allowed
     * @param monitor
     * @return widgetset module name (with package path using dots), null if no
     *         suitable location found
     * @throws CoreException
     */
    public static String getWidgetSet(IJavaProject project, boolean create,
            IPackageFragmentRoot root, String defaultPackage,
            IProgressMonitor monitor) throws CoreException {
        IPackageFragmentRoot[] packageFragmentRoots = project
                .getPackageFragmentRoots();

        // this duplicates some code with findWidgetSets with a few
        // modifications for efficiency - stop at first match and never continue
        // after that
        final StringBuilder wsPathBuilder = new StringBuilder();
        IResourceVisitor visitor = new IResourceVisitor() {
            boolean continueSearch = true;

            public boolean visit(IResource arg0) throws CoreException {
                if (arg0 instanceof IFile) {
                    IFile f = (IFile) arg0;
                    String name = f.getName();
                    if (name.endsWith(".gwt.xml")
                            && name.toLowerCase().contains("widgetset")) {
                        wsPathBuilder.append(f.getFullPath());
                        continueSearch = false;
                    }
                }
                return continueSearch;
            }
        };

        for (int i = 0; i < packageFragmentRoots.length; i++) {
            if (!packageFragmentRoots[i].isArchive()) {
                IResource underlyingResource = packageFragmentRoots[i]
                        .getUnderlyingResource();
                underlyingResource.accept(visitor);
                if (!wsPathBuilder.toString().equals("")) {
                    String wspath = wsPathBuilder.toString();
                    IPath fullPath = underlyingResource.getFullPath();
                    wspath = wspath.replace(fullPath.toString() + "/", "");
                    wspath = wspath.replaceAll("/", ".").replaceAll(".gwt.xml",
                            "");
                    return wspath;
                }
                // keep track of the first suitable package fragment root to use
                // as the default
                if (root == null) {
                    root = packageFragmentRoots[i];
                }
            }
        }

        /*
         * Project don't have custom widget set yet Come up with a default name
         * ( tool will create it later ). Find application classes and use the
         * one that has shortest package name. Add "WidgetSet" to that.
         */

        IType[] prospectClasses = VaadinPluginUtil.getApplicationClasses(
                project.getProject(), monitor);
        if (prospectClasses.length == 0) {
            // No application classes in project, try UIs
            prospectClasses = VaadinPluginUtil.getUiClasses(
                    project.getProject(), monitor);
        }

        if (defaultPackage == null) {
            String shortestPackagename = null;
            IType appWithShortestPackageName = null;
            for (int i = 0; i < prospectClasses.length; i++) {
                IType appclass = prospectClasses[i];
                String packagename = appclass.getPackageFragment().toString();
                if (shortestPackagename == null
                        || packagename.length() < shortestPackagename.length()) {
                    shortestPackagename = packagename;
                    appWithShortestPackageName = appclass;
                }
            }
            if (appWithShortestPackageName != null) {
                defaultPackage = appWithShortestPackageName
                        .getPackageFragment().getElementName() + ".widgetset";
                // find the package fragment root in which the application is
                // located to create the widgetset in the same source tree
                IPath path = appWithShortestPackageName.getPath();
                for (IPackageFragmentRoot newRoot = null; path.segmentCount() > 0; path = path
                        .removeLastSegments(1)) {
                    newRoot = project.findPackageFragmentRoot(path);
                    if (newRoot != null) {
                        root = newRoot;
                        break;
                    }
                }
            }
        }
        if (defaultPackage != null) {
            // Use project name for the widgetset by default
            String wsName = project.getProject().getName();

            // normalize in case not a valid Java identifier
            if (!Character.isJavaIdentifierStart(wsName.charAt(0))) {
                // add X to the beginning of the name
                wsName = "X" + wsName;
            } else {
                // uppercase first character
                wsName = wsName.substring(0, 1).toUpperCase()
                        + wsName.substring(1).toLowerCase();
            }
            // normalize a little
            wsName = wsName.replaceAll("[^A-Za-z_0-9]", "_");

            wsName += "Widgetset";
            String fullyQualifiedName = defaultPackage + "." + wsName;

            ErrorUtil.logInfo("No widget set found, " + fullyQualifiedName
                    + " will be created...");

            /* Update web.xml */
            if (hasWebXml(project)) {
                WebArtifactEdit artifact = WebArtifactEdit
                        .getWebArtifactEditForWrite(project.getProject());
                if (artifact == null) {
                    ErrorUtil.handleBackgroundException(
                            "Couldn't open web.xml for edit.", null);
                } else {
                    try {
                        WebXmlUtil.setWidgetSet(artifact, fullyQualifiedName,
                                Arrays.asList(prospectClasses));
                        artifact.saveIfNecessary(null);
                    } finally {
                        artifact.dispose();
                    }
                }
            } else {
                // possibly a Servlet 3.0 project with @WebServlet

                boolean updated = false;

                boolean vaadin71 = ProjectUtil.isVaadin71(project.getProject());
                // TODO progress monitor use
                if (vaadin71) {
                    IType[] servletClasses = VaadinPluginUtil
                            .getServletClasses(project.getProject(),
                                    new NullProgressMonitor());

                    for (IType servletClass : servletClasses) {
                        // if the servlet class has the
                        // @VaadinServletConfiguration
                        // annotation but no widgetset parameter, add the
                        // parameter
                        updated = updated
                                || updateServletAnnotationForWidgetset(project,
                                        servletClass, fullyQualifiedName,
                                        new NullProgressMonitor());
                    }
                }

                if (!updated) {
                    ErrorUtil
                    .displayWarningFromBackgroundThread(
                            "Update Widgetset",
                            "The widgetset "
                                    + fullyQualifiedName
                                    + " has been created but there is no web.xml to update.\n\n"
                                    + "Please update your @WebServlet or @"
                                    + VaadinPlugin.VAADIN_SERVLET_CONFIGURATION_ANNOTATION_NAME
                                    + " annotation parameters.");
                }
            }

            if (create) {
                if (root != null) {
                    // TODO monitor usage; test this
                    IPackageFragment fragment = root.createPackageFragment(
                            defaultPackage, true, monitor);
                    if (fragment.getResource() instanceof IFolder) {
                        IFolder wsFolder = (IFolder) fragment.getResource();
                        IFile file = wsFolder.getFile(wsName + ".gwt.xml");
                        String template = ProjectUtil.isVaadin7(project
                                .getProject()) ? "widgetsetxmlstub7.txt"
                                        : "widgetsetxmlstub6.txt";
                        VaadinPluginUtil.ensureFileFromTemplate(file, template);

                        // mark the created widgetset as dirty
                        WidgetsetUtil.setWidgetsetDirty(project.getProject(),
                                true);
                    }
                }
            }

            return fullyQualifiedName;
        }

        return null;
    }

    private static boolean updateServletAnnotationForWidgetset(
            IJavaProject project, IType servletClass, String widgetsetName,
            IProgressMonitor monitor) {
        boolean result = false;
        // check for and update @VaadinServletConfiguration
        try {

            monitor.beginTask("Modifying Servlet class", 5);

            ICompilationUnit compilationUnit = servletClass
                    .getCompilationUnit();

            String source = compilationUnit.getSource();
            Document document = new Document(source);

            compilationUnit
            .becomeWorkingCopy(new SubProgressMonitor(monitor, 1));

            try {
                ASTParser parser = ASTParser.newParser(AST.JLS3);
                parser.setSource(compilationUnit);
                parser.setResolveBindings(true);
                CompilationUnit astRoot = (CompilationUnit) parser
                        .createAST(new SubProgressMonitor(monitor, 1));

                astRoot.recordModifications();
                final AST ast = astRoot.getAST();

                // trick to get the annotation from the visitor
                final NormalAnnotation[] servletConfigurationAnnotation = new NormalAnnotation[1];
                servletConfigurationAnnotation[0] = null;

                astRoot.accept(new ASTVisitor() {

                    @Override
                    public boolean visit(NormalAnnotation annotation) {
                        ITypeBinding binding = annotation.resolveTypeBinding();
                        String fullyQualifiedName = binding.getQualifiedName();
                        if (VaadinPlugin.VAADIN_SERVLET_CONFIGURATION_ANNOTATION_FULL_NAME
                                .equals(fullyQualifiedName)) {
                            servletConfigurationAnnotation[0] = annotation;
                            return false;
                        }
                        return super.visit(annotation);
                    }

                    // Only look at top-level annotations
                    // TODO might be suboptimal if does not stop traversal early

                    @Override
                    public boolean visit(Block node) {
                        return false;
                    }

                    @Override
                    public boolean visit(MethodDeclaration node) {
                        return false;
                    }

                    @Override
                    public boolean visit(FieldDeclaration node) {
                        return false;
                    }
                });

                final StringLiteral widgetsetString = ast.newStringLiteral();
                widgetsetString.setLiteralValue(widgetsetName);

                final ImportRewrite importRewrite = ImportRewrite.create(
                        astRoot, true);

                if (servletConfigurationAnnotation[0] != null) {
                    // UPDATE existing annotation
                    // this is only done if there is no widgetset parameter
                    // explicitly defined
                    List values = servletConfigurationAnnotation[0].values();
                    MemberValuePair widgetsetPair = null;
                    for (int i = 0; i < values.size(); ++i) {
                        MemberValuePair pair = (MemberValuePair) values.get(i);
                        if (VaadinPlugin.VAADIN_SERVLET_CONFIGURATION_ANNOTATION_PARAMETER_WIDGETSET
                                .equals(pair.getName().getIdentifier())) {
                            widgetsetPair = pair;
                            break;
                        }
                    }
                    if (null == widgetsetPair) {
                        MemberValuePair pair = ast.newMemberValuePair();
                        final SimpleName attributeName = ast
                                .newSimpleName(VaadinPlugin.VAADIN_SERVLET_CONFIGURATION_ANNOTATION_PARAMETER_WIDGETSET);
                        pair.setName(attributeName);
                        pair.setValue(widgetsetString);
                        values.add(pair);

                        result = true;
                    }
                }

                TextEdit rewrite = astRoot.rewrite(document, compilationUnit
                        .getJavaProject().getOptions(true));
                try {
                    rewrite.apply(document);

                    // rewrite imports (after the body rewrite, modifies earlier
                    // part)
                    TextEdit rewriteImports = importRewrite
                            .rewriteImports(new SubProgressMonitor(monitor, 1));
                    rewriteImports.apply(document);
                } catch (Exception e) {
                    ErrorUtil.handleBackgroundException(IStatus.WARNING,
                            "Failed to update imports in the UI class "
                                    + servletClass.getFullyQualifiedName(), e);
                }
                String newSource = document.get();
                compilationUnit.getBuffer().setContents(newSource);

                // reconcile changes with other modifications to the same
                // compilation unit
                compilationUnit.reconcile(ICompilationUnit.NO_AST, false, null,
                        new SubProgressMonitor(monitor, 1));

                compilationUnit.commitWorkingCopy(false,
                        new SubProgressMonitor(monitor, 1));
            } finally {
                compilationUnit.discardWorkingCopy();
            }
        } catch (JavaModelException e) {
            ErrorUtil.handleBackgroundException(
                    "Failed to update @VaadinServletConfiguration annotation",
                    e);
            return false;
        } finally {
            monitor.done();
        }
        return result;
    }

    private static boolean hasWebXml(IJavaProject project) {
        boolean hasWebXml = false;
        WebArtifactEdit webArtifactEditForRead = WebArtifactEdit
                .getWebArtifactEditForRead(project.getProject());
        try {
            WebAppResource resource = (WebAppResource) (webArtifactEditForRead
                    .getDeploymentDescriptorResource());
            hasWebXml = !(null == resource || null == resource.getWebApp());
        } finally {
            if (null != webArtifactEditForRead) {
                webArtifactEditForRead.dispose();
            }
        }
        return hasWebXml;
    }

    /**
     * Returns jar files which contain widgetset for given project.
     * <p>
     * Method will iterate files in WEB-INF/lib and check each jar file there.
     *
     * @param jproject
     * @return
     * @throws CoreException
     */
    public static Collection<IPath> getAvailableVaadinWidgetsetPackages(
            IJavaProject jproject) throws CoreException {
        final Collection<IPath> vaadinpackages = new HashSet<IPath>();

        // IFolder webInfLibFolder =
        // ProjectUtil.getWebInfLibFolder(jproject.getProject());
        // if (!webInfLibFolder.exists()) {
        // return vaadinpackages;
        // }
        // webInfLibFolder.accept(new IResourceVisitor() {
        // public boolean visit(IResource resource) throws CoreException {
        // if (isNeededForWidgetsetCompilation(resource
        // .getProjectRelativePath())) {
        // vaadinpackages.add(resource.getRawLocation());
        // return true;
        // }
        // return true;
        // }
        // });

        // Iterate project classpath. Referenced gwt modules (without any server
        // side code like google-maps.jar) should not need to be in WEB-INF/lib,
        // but just manually added for project classpath
        IClasspathEntry[] rawClasspath = jproject.getRawClasspath();
        for (IClasspathEntry cp : rawClasspath) {
            if (cp.toString().contains(".jar")) {
                // User has explicitly defined GWT version to use directly on
                // the classpath, or classpath entry created by the plugin
                IClasspathEntry resolvedClasspathEntry = JavaCore
                        .getResolvedClasspathEntry(cp);
                IPath path = resolvedClasspathEntry.getPath();
                path = VaadinPluginUtil.makePathAbsolute(path);
                if (isNeededForWidgetsetCompilation(path)) {
                    vaadinpackages.add(path);
                }
            } else if (cp.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
                // primarily WEB-INF/lib, but possibly also Liferay etc.
                IClasspathContainer container = JavaCore.getClasspathContainer(
                        cp.getPath(), jproject);
                if (container == null) {
                    // Container is null at least for an invalid JRE reference
                    continue;
                }

                IClasspathEntry[] containerEntries = container
                        .getClasspathEntries();
                for (IClasspathEntry ccp : containerEntries) {
                    if (ccp.toString().contains(".jar")) {
                        IClasspathEntry resolvedClasspathEntry = JavaCore
                                .getResolvedClasspathEntry(ccp);
                        IPath path = resolvedClasspathEntry.getPath();
                        // IWorkspaceRoot root = ResourcesPlugin.getWorkspace()
                        // .getRoot();
                        path = VaadinPluginUtil.makePathAbsolute(path);
                        if (isNeededForWidgetsetCompilation(path)) {
                            vaadinpackages.add(path);
                        }
                    }
                }
            }
        }

        return vaadinpackages;
    }

    /**
     * Add widgetset nature to a project if not already there. Only modifies
     * Vaadin projects.
     *
     * @param project
     */
    public static void ensureWidgetsetNature(final IProject project) {
        if (!VaadinFacetUtils.isVaadinProject(project)) {
            return;
        }
        try {
            // Add nature if not there (to enable WidgetSet builder).
            // Nice when upgrading projects.
            IProjectNature nature = project
                    .getNature(WidgetsetNature.NATURE_ID);
            if (nature == null) {
                WidgetsetNature.addWidgetsetNature(project);
            }
        } catch (Exception e) {
            ErrorUtil.handleBackgroundException(IStatus.WARNING,
                    "Adding widgetset nature to the project failed.", e);
        }
    }

    public static IType[] getWidgetSetClasses(IProject project,
            IProgressMonitor monitor) throws JavaModelException {
        // find all non-binary subclasses of WidgetSet in the project
        return VaadinPluginUtil.getSubClasses(project,
                VaadinPlugin.VAADIN_PACKAGE_PREFIX
                + "terminal.gwt.client.WidgetSet", true, monitor);
    }

}
