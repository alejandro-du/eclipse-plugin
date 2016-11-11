package com.vaadin.integration.eclipse.util.network;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.data.MavenVaadinVersion;
import com.vaadin.integration.eclipse.util.files.LocalFileManager;
import com.vaadin.integration.eclipse.util.files.LocalFileManager.FileType;
import com.vaadin.integration.eclipse.wizards.VaadinArchetype;

public class MavenVersionManager {

    private static final String VERSIONS_FILE_NAME = "VERSIONS";

    private static final String ARCHETYPES_FILE_NAME = "eclipse-maven-archetypes.xml";

    private static final String AVAILABLE_VAADIN_VERSIONS_URL = DownloadManager.VAADIN_DOWNLOAD_BASE_URL
            + VERSIONS_FILE_NAME;

    private static final String AVAILABLE_VAADIN_ARCHETYPES_URL = DownloadManager.VAADIN_DOWNLOAD_BASE_URL
            + ARCHETYPES_FILE_NAME;

    private static List<MavenVaadinVersion> availableVersions;

    private static List<VaadinArchetype> allArchetypes;

    /**
     * Returns a list of available Vaadin archetypes. It is not guaranteed that
     * the list is fetched from the site every time this is called.
     *
     * @param includePrereleases
     *            true to also return pre-release versions of archetypes
     * @param versionRegex
     *            regular expression used for matching correct version
     *            attributes in the archetype
     * @return A sorted list of available Vaadin archetypes
     * @throws CoreException
     */
    @SuppressWarnings("restriction")
    public static synchronized List<VaadinArchetype> getAvailableArchetypes(
            boolean includePrereleases, String versionRegex) {
        if (allArchetypes == null) {
            try {
                loadAndCacheResource(AVAILABLE_VAADIN_ARCHETYPES_URL,
                        ARCHETYPES_FILE_NAME);
            } catch (IOException e) {
                ErrorUtil.handleBackgroundException(
                        "Failed to retrieve Vaadin archetype list from server",
                        e);
            }
            try {
                allArchetypes = loadCachedArchetypes(ARCHETYPES_FILE_NAME);
            } catch (CoreException e) {
                ErrorUtil.handleBackgroundException(
                        "Failed to load cached Vaadin archetypes", e);
            }
        }

        if (allArchetypes == null) {
            allArchetypes = loadDefaultArchetypes();
        }

        List<VaadinArchetype> availableArchetypes = new ArrayList<VaadinArchetype>();
        for (VaadinArchetype vaadinArchetype : allArchetypes) {
            if (vaadinArchetype.getArchetype().getVersion()
                    .matches(versionRegex)) {
                if (includePrereleases || !vaadinArchetype.isPrerelease()) {
                    availableArchetypes.add(vaadinArchetype);
                }
            }
        }

        return availableArchetypes;
    }

    private static List<VaadinArchetype> loadCachedArchetypes(String filename)
            throws CoreException {
        File file = getCacheFile(filename);
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(file));
            return parseArchetypesStream(is);
        } catch (IOException ignored) {
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    private static List<VaadinArchetype> loadDefaultArchetypes() {
        InputStream is = MavenVersionManager.class
                .getResourceAsStream("/resources/default-maven-archetypes.xml");
        try {
            return parseArchetypesStream(is);
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                ErrorUtil.handleBackgroundException(
                        "Failed to load cached Vaadin archetypes", e);
            }
        }

    }

    private static List<VaadinArchetype> parseArchetypesStream(InputStream is) {
        List<VaadinArchetype> result = new ArrayList<VaadinArchetype>();
        try {
            Document parsedArchetypeList = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().parse(is);
            NodeList nodeList = parsedArchetypeList.getDocumentElement()
                    .getElementsByTagName("archetype");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element archetypeNode = (Element) nodeList.item(i);
                String archetypeId = archetypeNode.getAttribute("archetypeId");
                String groupId = archetypeNode.getAttribute("groupId");
                String version = archetypeNode.getAttribute("version");
                String title = archetypeNode.getElementsByTagName("title")
                        .item(0).getTextContent();
                String description = archetypeNode
                        .getElementsByTagName("description").item(0)
                        .getTextContent();
                boolean prerelease = archetypeNode.hasAttribute("prerelease")
                        && "true".equals(archetypeNode.getAttribute("prerelease"));
                result.add(new VaadinArchetype(title, archetypeId, groupId,
                        version, description, prerelease));
            }
        } catch (Exception e) {
            ErrorUtil.handleBackgroundException(
                    "Failed to parse archetype list", e);
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * Returns a list of what Vaadin versions are available for dependency
     * management systems. The list contains release version and additionally,
     * if onlyRelease is false, nightly and pre-release versions.
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
    public static synchronized List<MavenVaadinVersion> getAvailableVersions(
            boolean onlyRelease) throws CoreException {
        if (availableVersions == null) {
            try {
                availableVersions = downloadAvailableVersionsList();
            } catch (CoreException e) {
                ErrorUtil.handleBackgroundException(
                        "Failed to retrieve available Vaadin 7 version list from server, using cached list",
                        e);
                availableVersions = getCachedAvailableVersionsList();
            }
        }

        List<MavenVaadinVersion> versions;
        if (onlyRelease) {
            // Filter out non-releases
            versions = new ArrayList<MavenVaadinVersion>();
            for (MavenVaadinVersion version : availableVersions) {
                if (version.getType() == FileType.VAADIN_RELEASE) {
                    versions.add(version);
                }
            }
        } else {
            // Return everything
            versions = new ArrayList<MavenVaadinVersion>(availableVersions);
        }
        return versions;
    }

    /**
     * Download and return the list of available Vaadin versions from vaadin.com
     * .
     *
     * If the download succeeds, also save the list in the cache.
     *
     * @return
     * @throws CoreException
     */
    private static List<MavenVaadinVersion> downloadAvailableVersionsList()
            throws CoreException {
        try {
            String versionData = loadAndCacheResource(
                    AVAILABLE_VAADIN_VERSIONS_URL, VERSIONS_FILE_NAME);
            return parseAvailableVersions(versionData);
        } catch (IOException e) {
            throw ErrorUtil.newCoreException(
                    "Failed to download list of available Vaadin versions", e);
        }
    }

    private static String loadAndCacheResource(String url, String cacheFileName)
            throws IOException {
        String data = DownloadManager.downloadURL(url);

        // store downloaded data to cache
        try {
            File cacheFile = getCacheFile(cacheFileName);
            cacheFile.getParentFile().mkdirs();
            BufferedWriter writer = new BufferedWriter(
                    new FileWriter(cacheFile));
            try {
                writer.write(data);
            } finally {
                writer.close();
            }
        } catch (CoreException e) {
            // log and ignore - the data is still valid
            ErrorUtil.handleBackgroundException(
                    "Failed to save " + url + " to cache", e);
        } catch (IOException e) {
            // log and ignore - the data is still valid
            ErrorUtil.handleBackgroundException(
                    "Failed to save " + url + " to cache", e);
        }
        return data;
    }

    /**
     * Return the cached list of available Vaadin versions from last successful
     * request to vaadin.com .
     *
     * @return
     * @throws CoreException
     */
    private static List<MavenVaadinVersion> getCachedAvailableVersionsList()
            throws CoreException {
        try {
            File cacheFile = getCacheFile(VERSIONS_FILE_NAME);
            InputStream is = new FileInputStream(cacheFile);
            String versionData;
            try {
                versionData = IOUtils.toString(is);
            } finally {
                is.close();
            }

            return parseAvailableVersions(versionData);
        } catch (IOException e) {
            throw ErrorUtil.newCoreException(
                    "Failed to get cached list of available Vaadin versions",
                    e);
        }
    }

    private static File getCacheFile(String fileName) throws CoreException {
        IPath path = LocalFileManager.getConfigurationPath()
                .append(IPath.SEPARATOR + fileName);
        return path.toFile();
    }

    /**
     * Parses the available versions and URLs from comma separated data.
     *
     * Anything after a comma is ignored, as are comment rows. A row starting
     * with a comma can be used in future file versions for information
     * incompatible with this plug-in version.
     *
     * @param versionData
     * @return
     */
    private static List<MavenVaadinVersion> parseAvailableVersions(
            String versionData) {
        List<MavenVaadinVersion> availableVersions = new ArrayList<MavenVaadinVersion>();

        String[] rows = versionData.split("(\r|\n)");
        for (String row : rows) {
            String[] data = row.split(",");
            if (data.length == 0 || data[0].startsWith("#")) {
                // Skip unknown data
                continue;
            }

            // in this version, ignore anything after a comma
            String versionNumber = data[0].trim();
            if (!"".equals(versionNumber)) {
                MavenVaadinVersion vaadinVersion = new MavenVaadinVersion(
                        versionNumber);
                availableVersions.add(vaadinVersion);
            }
        }

        return availableVersions;
    }

}
