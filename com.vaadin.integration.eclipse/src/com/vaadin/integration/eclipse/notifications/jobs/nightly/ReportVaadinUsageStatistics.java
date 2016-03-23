package com.vaadin.integration.eclipse.notifications.jobs.nightly;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.ProjectUtil;
import com.vaadin.integration.eclipse.util.VaadinPluginUtil;

/**
 * User-visible job that reports usage statistics for projects in workspace.
 */
public abstract class ReportVaadinUsageStatistics extends Job {

    public static final String ANONYMOUS_ID = loadFirstLaunch();

    private static final String COMPILER = "Compiler"; //$NON-NLS-1$

    private static final String E_QPARAM = "&e="; //$NON-NLS-1$

    private static final String R_QPARAM = "&r=unknown"; //$NON-NLS-1$

    private static final String ID_QPARAM = "&id="; //$NON-NLS-1$

    private static final String V_QPARAM = "?v="; //$NON-NLS-1$

    private static final String BUNDLE_VERSION = "Bundle-Version"; //$NON-NLS-1$

    private static final String ECLIPSE_IVM = " Eclipse IVM: "; //$NON-NLS-1$

    private static final String ECLIPSE_PLUGIN = "Eclipse Plugin "; //$NON-NLS-1$

    private static final String OS_VERSION = "os.version"; //$NON-NLS-1$

    private static final String OS_NAME = "os.name"; //$NON-NLS-1$

    private static final String OS_ARCH = "os.arch"; //$NON-NLS-1$

    private static final String JAVA_VERSION = "java.version"; //$NON-NLS-1$

    private static final String JAVA_VENDOR = "java.vendor"; //$NON-NLS-1$

    private static final String USER_AGENT = "User-Agent"; //$NON-NLS-1$

    // Use the GWT Freshness checker URL to store usage reports.
    private static final String QUERY_URL = "https://tools.vaadin.com/version/currentversion.xml"; //$NON-NLS-1$

    private static final String FIRST_LAUNCH = "firstLaunch"; //$NON-NLS-1$

    ReportVaadinUsageStatistics() {
        super(Messages.Notifications_UsageStatJobName);
        setUser(false);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        try {
            monitor.beginTask(Messages.Notifications_UsageStatTask,
                    IProgressMonitor.UNKNOWN);

            if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            return reportUsage(monitor);
        } finally {
            monitor.done();
        }
    }

    private IStatus reportUsage(IProgressMonitor monitor) {
        // map from project with "use latest nightly" to the current
        // Vaadin version number string in the project
        Map<IProject, String> vaadinProjects = getVaadinProjects();
        monitor.worked(1);

        if (vaadinProjects.isEmpty()) {
            return Status.OK_STATUS;
        } else if (monitor.isCanceled()) {
            return Status.CANCEL_STATUS;
        }

        try {
            IStatus status = doReportUsage(vaadinProjects, monitor);
            monitor.worked(1);
            return status;
        } finally {
            monitor.done();
        }
    }

    /**
     * Returns the open projects in the workspace which use Vaadin
     * 
     * @return
     */
    private Map<IProject, String> getVaadinProjects() {
        Map<IProject, String> projectsWithVaadin = new HashMap<IProject, String>();
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IProject[] projects = workspaceRoot.getProjects();
        for (IProject project : projects) {
            try {
                if (project.isOpen() && project.hasNature(JavaCore.NATURE_ID)) {
                    String versionNumber = ProjectUtil.getVaadinLibraryVersion(
                            project, true);
                    if (null != versionNumber) {
                        projectsWithVaadin.put(project, versionNumber);
                    }
                }

            } catch (CoreException e) {
                ErrorUtil.handleBackgroundException(IStatus.WARNING,
                        "Could not check Vaadin version in project " //$NON-NLS-1$
                                + project.getName(),
                        e);
            }
        }
        return projectsWithVaadin;
    }

    private IStatus doReportUsage(Map<IProject, String> projects,
            IProgressMonitor monitor) {
        int size = projects.size() * 2;
        SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, size);
        subMonitor.beginTask(Messages.Notifications_UsageStatTaskName, size);
        try {
            for (Entry<IProject, String> entry : projects.entrySet()) {

                if (monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }

                IJavaProject jProject;
                IVMInstall ivmInstall = null;
                try {
                    // attempt to get ijavaproject from iproject, this fails if
                    // the project has no java nature.
                    // a vaadin project without a java nature should not exist
                    // so this should always succeed.
                    jProject = (IJavaProject) entry.getKey()
                            .getNature(JavaCore.NATURE_ID);
                    ivmInstall = VaadinPluginUtil.getJvmInstall(jProject,
                            false);
                } catch (CoreException e) {
                    // this should never happen as long as all vaadin projects
                    // use java
                    ErrorUtil.handleBackgroundException(
                            "Could not find IVM for Vaadin Project", e); //$NON-NLS-1$
                }
                subMonitor.worked(1);
                report(entry.getValue(), makeUserAgent(ivmInstall));
                subMonitor.worked(1);
            }
            return Status.OK_STATUS;
        } finally {
            subMonitor.done();
        }
    }

    private void report(String version, String userAgent) {
        StringBuilder url = new StringBuilder(QUERY_URL);
        url.append(V_QPARAM);
        url.append(version);
        url.append(ID_QPARAM);
        url.append(ANONYMOUS_ID).append(R_QPARAM);

        // TODO add more meaningful revision parameter if possible
        String entryPoint = COMPILER; // TODO add more relevant entry point if
                                      // feasible
        if (entryPoint != null) {
            url.append(E_QPARAM).append(entryPoint);
        }

        doHttpGet(userAgent, url.toString());
    }

    private void doHttpGet(String userAgent, String url) {
        Throwable caught;
        InputStream is = null;
        try {
            URL urlToGet = new URL(url);
            URLConnection conn = urlToGet.openConnection();
            conn.setRequestProperty(USER_AGENT, userAgent);
            is = conn.getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            baos.toByteArray();
            return;
        } catch (MalformedURLException e) {
            caught = e;
        } catch (IOException e) {
            caught = e;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }

        // Do not this error anyhow in UI, just log it.
        Logger.getLogger(ReportVaadinUsageStatistics.class.getName()).log(
                Level.INFO, "Caught an exception while executing HTTP query", //$NON-NLS-1$
                caught);

        return;
    }

    private String makeUserAgent(IVMInstall ivmInstall) {
        StringBuilder userAgent = new StringBuilder(ECLIPSE_PLUGIN);
        userAgent.append(getPluginVersion());

        StringBuilder extra = new StringBuilder();
        appendUserAgentProperty(extra, JAVA_VENDOR);
        appendUserAgentProperty(extra, JAVA_VERSION);
        appendUserAgentProperty(extra, OS_ARCH);
        appendUserAgentProperty(extra, OS_NAME);
        appendUserAgentProperty(extra, OS_VERSION);

        if (extra.length() > 0) {
            userAgent.append(' ').append('(');
            userAgent.append(extra);
            userAgent.append(')');
        }
        String ivmName = ivmInstall == null ? "" : ivmInstall.getName(); //$NON-NLS-1$
        userAgent.append(ECLIPSE_IVM);
        userAgent.append(ivmName);

        return userAgent.toString();
    }

    private String getPluginVersion() {
        String version = Platform.getBundle(VaadinPlugin.PLUGIN_ID).getHeaders()
                .get(BUNDLE_VERSION);
        return version;
    }

    private void appendUserAgentProperty(StringBuilder sb, String propName) {
        String propValue = System.getProperty(propName);
        if (propValue != null) {
            if (sb.length() > 0) {
                sb.append(';').append(' ');
            }
            sb.append(propName);
            sb.append('=');
            sb.append(propValue);
        }
    }

    private static String loadFirstLaunch() {
        Preferences prefs;
        prefs = Preferences
                .userNodeForPackage(ReportVaadinUsageStatistics.class);

        long currentTimeMillis = System.currentTimeMillis();
        String firstLaunch = prefs.get(FIRST_LAUNCH, null);
        if (firstLaunch == null) {
            firstLaunch = Long.toHexString(currentTimeMillis);
            prefs.put(FIRST_LAUNCH, firstLaunch);
        }
        return firstLaunch;

    }

}