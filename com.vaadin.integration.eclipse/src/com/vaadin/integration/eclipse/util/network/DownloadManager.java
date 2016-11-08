package com.vaadin.integration.eclipse.util.network;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.LegacyUtil;
import com.vaadin.integration.eclipse.util.VersionUtil;
import com.vaadin.integration.eclipse.util.data.DownloadableVaadinVersion;
import com.vaadin.integration.eclipse.util.files.LocalFileManager;
import com.vaadin.integration.eclipse.util.files.LocalFileManager.FileType;

public class DownloadManager {

    public static final String VAADIN_DOWNLOAD_BASE_URL = "http://vaadin.com/download/";

    public static final String VAADIN_LATEST_URL = VAADIN_DOWNLOAD_BASE_URL
            + "LATEST";

    private static final String AVAILABLE_VAADIN_VERSIONS_ALL_URL = VAADIN_DOWNLOAD_BASE_URL
            + "VERSIONS_ALL";

    private static final String GWT_DOWNLOAD_URL = "http://vaadin.com/download/external/gwt";

    private static List<DownloadableVaadinVersion> availableVersions;

    /**
     * Returns the latest release version available to download.
     * 
     * @return Version string for the latest Vaadin release version.
     * @throws CoreException
     *             If the latest version could not be checked due to network
     *             issues
     */
    public static String getLatestVaadinVersion() throws CoreException {
        // TODO: Could be cached for better performance if this is called often
        try {
            String version = downloadURL(VAADIN_LATEST_URL);

            // The first row contains the version string
            version = version.replaceAll("[\r\n].*", "");
            return version;
        } catch (Exception e) {
            throw ErrorUtil.newCoreException(
                    "Checking latest available version of Vaadin failed", e);
        }
    }

    /**
     * Returns the contents of the file the URL refers to.
     * 
     * @param fileURL
     *            The URL location of the file
     * @return The contents of the given URL
     * @throws IOException
     *             Thrown if there was a network problem or a problem with the
     *             URL
     */
    static String downloadURL(String url) throws IOException {
        return IOUtils.toString(getDownloadStream(url));
    }

    /**
     * Returns an InputStream for the given URL. Takes care of proxies etc.
     * 
     * @param urlString
     * @return
     * @throws IOException
     */
    private static InputStream getDownloadStream(String urlString)
            throws IOException {
        URL url = new URL(urlString);
        InputStream inputStream = url.openStream();

        return inputStream;
    }

    /**
     * Returns a list of what Vaadin versions are available for download. The
     * list contains release version and additionally, if includeDevelopment is
     * true, nightly and pre-release versions.
     * 
     * It is not guaranteed that the list is fetched from the site every time
     * this is called.
     * 
     * @param onlyRelease
     *            True to include only release builds, false to include others
     *            also (nightly, pre-release)
     * @return A sorted list of available Vaadin versions
     * @throws CoreException
     * 
     */
    public static synchronized List<DownloadableVaadinVersion> getAvailableVersions(
            boolean onlyRelease) throws CoreException {
        if (availableVersions == null) {
            availableVersions = downloadAvailableVersionsList();
        }

        List<DownloadableVaadinVersion> versions;
        if (onlyRelease) {
            // Filter out non-releases
            versions = new ArrayList<DownloadableVaadinVersion>();
            for (DownloadableVaadinVersion version : availableVersions) {
                if (version.getType() == FileType.VAADIN_RELEASE) {
                    versions.add(version);
                }
            }
        } else {
            // Return everything
            versions = new ArrayList<DownloadableVaadinVersion>(
                    availableVersions);
        }
        return versions;

    }

    /**
     * Flush the cached list of versions, forcing it to be reloaded the next
     * time it is requested.
     */
    public static synchronized void flushCache() {
        availableVersions = null;
    }

    /**
     * Download and return the list of available Vaadin versions from vaadin.com
     * 
     * @return
     * @throws CoreException
     */
    private static List<DownloadableVaadinVersion> downloadAvailableVersionsList()
            throws CoreException {
        try {
            String versionData = downloadURL(AVAILABLE_VAADIN_VERSIONS_ALL_URL);
            return parseAvailableVersions(versionData);
        } catch (IOException e) {
            throw ErrorUtil.newCoreException(
                    "Failed to download list of available Vaadin versions", e);
        }
    }

    /**
     * Parses the available versions and URLs from comma separated data.
     * 
     * @param versionData
     * @return
     */
    private static List<DownloadableVaadinVersion> parseAvailableVersions(
            String versionData) {
        List<DownloadableVaadinVersion> availableVersions = new ArrayList<DownloadableVaadinVersion>();

        String[] rows = versionData.split("(\r|\n)");
        for (String row : rows) {
            String[] data = row.split(",");
            if (data.length < 3) {
                // Skip unknown data
                continue;
            }

            FileType type = FileType.getVaadinReleaseType(data[0]);
            String versionNumber = data[1];
            // Vaadin 7 only via dependency management - see MavenVersionManager
            if (VersionUtil.isVaadin7VersionString(versionNumber)) {
                continue;
            }
            String downloadUrl = data[2];
            DownloadableVaadinVersion vaadinVersion = new DownloadableVaadinVersion(
                    versionNumber, type, downloadUrl);
            availableVersions.add(vaadinVersion);
        }

        return availableVersions;
    }

    /**
     * Download Vaadin JAR with the given version number.
     * 
     * @param versionNumber
     * @param subProgressMonitor
     * @throws CoreException
     */
    public static void downloadVaadin(String versionNumber,
            IProgressMonitor subProgressMonitor) throws CoreException {
        List<DownloadableVaadinVersion> versions = getAvailableVersions(false);
        DownloadableVaadinVersion downloadableVersion = null;
        for (DownloadableVaadinVersion v : versions) {
            if (v.getVersionNumber().equals(versionNumber)) {
                downloadableVersion = v;
                break;
            }
        }

        if (downloadableVersion != null) {
            try {
                downloadFileToLocalStore(downloadableVersion.getDownloadURL(),
                        downloadableVersion.getType(), versionNumber,
                        subProgressMonitor);
            } catch (CoreException e) {
                throw ErrorUtil.newCoreException(
                        "Failed to download Vaadin version ("
                                + downloadableVersion.getVersionNumber(), e);
            }
        } else {
            throw ErrorUtil
                    .newCoreException("Unable to find requested version ("
                            + versionNumber + ")");
        }

    }

    private static final String GWT_USER_JAR_DOWNLOAD_URL(String version) {
        return GWT_DOWNLOAD_URL + "/" + version + "/gwt-user.jar";
    }

    private static final String GWT_DEV_JAR_DOWNLOAD_URL(String version) {
        String versionUrl = GWT_DOWNLOAD_URL + "/" + version;
        String filename = FileType.GWT_DEV_JAR.getFilename(version);
        if (LegacyUtil.isPlatformDependentGWT(version)) {
            filename = FileType.GWT_DEV_JAR_PLATFORM_DEPENDENT
                    .getFilename(version);
        }
        return versionUrl + "/" + filename;
    }

    private static String GWT_DEPENDENCY_JAR_DOWNLOAD_URL(String gwtVersion,
            String dependencyJar) {
        return GWT_DOWNLOAD_URL + "/" + gwtVersion + "/" + dependencyJar;
    }

    /**
     * Downloads the gwt-user.jar for the specified GWT version if it has not
     * already been dowloaded.
     * 
     * @param version
     * @param monitor
     * @throws CoreException
     */
    public static void downloadGwtUserJar(String gwtVersion,
            IProgressMonitor monitor) throws CoreException {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        try {
            String url = GWT_USER_JAR_DOWNLOAD_URL(gwtVersion);
            downloadFileToLocalStore(url, FileType.GWT_USER_JAR, gwtVersion,
                    monitor);
        } finally {
            monitor.done();
        }
    }

    /**
     * Downloads the gwt-dev.jar for the specified GWT version if it has not
     * already been dowloaded.
     * 
     * @param version
     * @param monitor
     * @throws CoreException
     */
    public static void downloadGwtDevJar(String gwtVersion,
            IProgressMonitor monitor) throws CoreException {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }

        try {
            String url = GWT_DEV_JAR_DOWNLOAD_URL(gwtVersion);
            FileType fileType;
            if (LegacyUtil.isPlatformDependentGWT(gwtVersion)) {
                fileType = FileType.GWT_DEV_JAR_PLATFORM_DEPENDENT;
            } else {
                fileType = FileType.GWT_DEV_JAR;
            }

            downloadFileToLocalStore(url, fileType, gwtVersion, monitor);
        } finally {
            monitor.done();
        }
    }

    public static void downloadDependency(String gwtVersion,
            String dependencyJar, IProgressMonitor monitor)
            throws CoreException {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }

        try {
            String url = GWT_DEPENDENCY_JAR_DOWNLOAD_URL(gwtVersion,
                    dependencyJar);

            IPath target = LocalFileManager.getLocalGWTDependencyJar(
                    gwtVersion, dependencyJar);
            downloadFileToLocalStore(url, target.toFile());
        } finally {
            monitor.done();
        }

    }

    /**
     * Downloads the url and stores it in the local file store as the selected
     * type and version. Silently returns false if the file already exists in
     * the local file store.
     * 
     * @param url
     * @param fileType
     * @param version
     * @param monitor
     * @return true if the file was downloaded, false if the file already
     *         existed in the local system
     * @throws CoreException
     */
    private static boolean downloadFileToLocalStore(String url,
            FileType fileType, String version, IProgressMonitor monitor)
            throws CoreException {
        String filename = fileType.getFilename(version);
        monitor.beginTask("Downloading " + filename + "...",
                IProgressMonitor.UNKNOWN);

        File targetFile = LocalFileManager.getLocalFile(fileType, version)
                .toFile();

        return downloadFileToLocalStore(url, targetFile);
    }

    /**
     * Downloads the file from the given url and stores it as targetFile. Does
     * nothing if targetFile already exists.
     * 
     * @param url
     * @param targetFile
     * @return true if the file was downloaded, false otherwise
     * @throws CoreException
     *             If the download failed
     */
    private static boolean downloadFileToLocalStore(String url, File targetFile)
            throws CoreException {
        if (targetFile.exists()) {
            return false;
        }

        try {
            // Create the parent directory if it does not exist
            if (!targetFile.getParentFile().exists()) {
                targetFile.getParentFile().mkdirs();
            }

            IOUtils.copy(getDownloadStream(url), new FileOutputStream(
                    targetFile));

            return true;
        } catch (IOException e) {
            throw ErrorUtil.newCoreException("Failed to download "
                    + targetFile.getName() + " from " + url);
        }

    }

}
