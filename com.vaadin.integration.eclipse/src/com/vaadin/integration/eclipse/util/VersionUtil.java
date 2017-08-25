package com.vaadin.integration.eclipse.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;

import com.vaadin.integration.eclipse.util.data.AbstractVaadinVersion;

public class VersionUtil {

    private static final String GWT_VERSION_DEPENDENCIES_ATTRIBUTE = "GWT-Version-Dependencies";
    private static final String VAADIN_VERSION_PATTERN = "([0-9]*)\\.([0-9])\\.(.+)";
    public static final String VAADIN_JAR_REGEXP = "^vaadin-(server-)?"
            + VAADIN_VERSION_PATTERN + "\\.jar$";

    /**
     * Returns the standard filename of the vaadin jar with the given version.
     * 
     * @param vaadinJarVersion
     *            Version string
     * @return The full jar name of the Vaadin jar
     */
    public static String getVaadinJarFilename(String vaadinJarVersion) {
        // Vaadin JAR filename is always "vaadin-<version>.jar"
        return "vaadin-" + vaadinJarVersion + ".jar";
    }

    /**
     * Returns the Vaadin version for the given Vaadin jar
     * 
     * @param resource
     * @return The version string or null if the version could not be
     *         determined.
     */
    public static String getVaadinVersionFromJar(IPath resource) {
        if (resource == null || !resource.toPortableString().endsWith(".jar")
                || !resource.toFile().exists()) {
            return null;
        }
        JarFile jarFile = null;
        try {
            URL url = new URL("file:" + resource.toPortableString());
            url = new URL("jar:" + url.toExternalForm() + "!/");
            JarURLConnection conn = (JarURLConnection) url.openConnection();
            jarFile = conn.getJarFile();

            // Try to get version from manifest (in Vaadin 6.4.6 and newer)
            String versionString = getManifestVaadinVersion(jarFile);
            if (versionString != null) {
                return versionString;
            }

            // Try to get version from META-INF/VERSION
            ZipEntry entry = jarFile.getEntry("META-INF/VERSION");
            if (entry != null) {
                InputStream inputStream = jarFile.getInputStream(entry);
                versionString = new BufferedReader(new InputStreamReader(
                        inputStream)).readLine();
                inputStream.close();
            }

            return versionString;
        } catch (Throwable t) {
            ErrorUtil.handleBackgroundException(IStatus.INFO,
                    "Could not access JAR when checking for Vaadin version", t);
        } finally {
            VaadinPluginUtil.closeJarFile(jarFile);
        }
        return null;
    }

    /**
     * Returns the Vaadin JAR version, as specified in the manifest.
     * 
     * @param jarFile
     *            A JarFile reference to a vaadin jar. Not closed by the method,
     *            needs to be closed afterwards. Must not be null.
     * @return The Vaadin version stated in the manifest or null if not found.
     * @throws IOException
     */
    private static String getManifestVaadinVersion(JarFile jarFile)
            throws IOException {
        String version = getManifestVersion(jarFile, "Implementation-Version");
        // Check that the version string is of the expected form.
        // For instance SNAPSHOT versions won't pass this check.
        if (version != null && version.matches("\\d+.\\d+.\\d+(\\..*)?")) {
            return version;
        }
        // Otherwise fall back to using Bundle-Version.
        return getManifestVersion(jarFile, "Bundle-Version");
    }

    /**
     * Returns the GWT version required by the Vaadin JAR, as specified in the
     * manifest.
     * 
     * @param jarFile
     *            A JarFile reference to a vaadin jar. Not closed by the method,
     *            needs to be closed afterwards. Must not be null.
     * @return The Vaadin version stated in the manifest or null if not found.
     * @throws IOException
     */
    private static String getManifestGWTVersion(JarFile jarFile)
            throws IOException {
        return getManifestVersion(jarFile, "GWT-Version");
    }

    private static String getManifestVersion(JarFile jarFile,
            String versionAttribute) throws IOException {
        Manifest manifest = jarFile.getManifest();
        if (manifest == null) {
            return null;
        }
        Attributes attr = manifest.getMainAttributes();
        String bundleName = attr.getValue("Bundle-Name");
        if (bundleName != null
                && (bundleName.startsWith("Vaadin") || bundleName
                        .startsWith("vaadin-"))) {
            return attr.getValue(versionAttribute);
        }

        return null;
    }

    private static List<String> getManifestArrayAttribute(JarFile jarFile,
            String attributeName) throws IOException {
        Manifest manifest = jarFile.getManifest();
        if (manifest == null) {
            return null;
        }
        Attributes attr = manifest.getMainAttributes();
        String commaSeparatedValue = attr.getValue(attributeName);
        ArrayList<String> result = new ArrayList<String>();
        if (commaSeparatedValue != null) {
            for (String value : commaSeparatedValue.split(",")) {
                result.add(value.trim());
            }
        }

        return result;
    }

    /**
     * Checks if a file with the given name could be a Vaadin Jar. The file does
     * not necessary exist so only a name based check is done.
     * 
     * @param name
     * @return
     */
    public static boolean couldBeOfficialVaadinJar(String name) {
        // Official Vaadin jars are named vaadin-<version>.jar
        // <version> should always start with a number. Failing to check this
        // will return true for e.g. vaadin-treetable-1.0.0.jar

        return name.matches(VAADIN_JAR_REGEXP);
    }

    /**
     * Returns the GWT version required by the given vaadin jar.
     * 
     * @param vaadinJarPath
     *            The path of Vaadin jar, must not be null.
     * @return The required GWT version or null if it could not be determined
     * @throws IOException
     */
    public static String getRequiredGWTVersionForVaadinJar(IPath vaadinJarPath)
            throws IOException {

        File vaadinJarFile = vaadinJarPath.toFile();
        if (vaadinJarFile == null || !vaadinJarFile.exists()) {
            return null;
        }

        // Check gwt version from included Vaadin jar
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(vaadinJarFile);
            // Check GWT version from manifest
            String manifestGWTVersion = getManifestGWTVersion(jarFile);
            if (manifestGWTVersion != null) {
                return manifestGWTVersion;
            }

            ZipEntry entry = jarFile.getEntry("META-INF/GWT-VERSION");
            if (entry == null) {
                // found JAR but not GWT version information in it, use
                // default
                return null;
            }

            // extract GWT version from the JAR
            InputStream gwtVersionStream = jarFile.getInputStream(entry);
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    gwtVersionStream));

            String gwtVersion = reader.readLine();
            return gwtVersion;
        } finally {
            if (jarFile != null) {
                VaadinPluginUtil.closeJarFile(jarFile);
            }
        }
    }

    public static List<String> getRequiredGWTDependenciesForVaadinJar(
            IPath vaadinJarPath) throws IOException {
        File vaadinJarFile = vaadinJarPath.toFile();
        if (vaadinJarFile == null || !vaadinJarFile.exists()) {
            return null;
        }

        // Check gwt version from included Vaadin jar
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(vaadinJarFile);
            // Check GWT version from manifest
            return getManifestArrayAttribute(jarFile,
                    GWT_VERSION_DEPENDENCIES_ATTRIBUTE);
        } finally {
            if (jarFile != null) {
                VaadinPluginUtil.closeJarFile(jarFile);
            }
        }
    }

    /**
     * Checks if the Vaadin version is 7 or higher.
     * 
     * If major version cannot be determined, false is returned.
     * 
     * @param vaadinVersion
     * @return
     */
    public static boolean isVaadin7(AbstractVaadinVersion vaadinVersion) {
        return isVaadin7VersionString(vaadinVersion.getVersionNumber());
    }

    /**
     * Checks if the Vaadin version is 7.1 or higher.
     * 
     * If major or minor version cannot be determined, false is returned.
     * 
     * @param vaadinVersion
     * @return
     */
    public static boolean isVaadin71(AbstractVaadinVersion vaadinVersion) {
        return isVaadin71VersionString(vaadinVersion.getVersionNumber());
    }

    /**
     * Checks if the Vaadin version is 7.3 or higher.
     * 
     * If major or minor version cannot be determined, false is returned.
     * 
     * @param vaadinVersion
     * @return
     */
    public static boolean isVaadin73(AbstractVaadinVersion vaadinVersion) {
        return isVaadin73VersionString(vaadinVersion.getVersionNumber());
    }

    /**
     * Checks if the Vaadin version is 7 or higher.
     * 
     * If major version cannot be determined, false is returned.
     * 
     * @param vaadinVersion
     * @return
     */
    public static boolean isVaadin7VersionString(String vaadinVersion) {
        return isAtLeastVersionString(vaadinVersion, 7, 0);
    }

    public static boolean isVaadin71VersionString(String vaadinVersion) {
        return isAtLeastVersionString(vaadinVersion, 7, 1);
    }

    public static boolean isVaadin73VersionString(String vaadinVersion) {
        return isAtLeastVersionString(vaadinVersion, 7, 3);
    }

    /**
     * Checks whether version string represents a stable version, as opposed to
     * snapshots and alphas, betas and release candidates. A stable version
     * contains only numeric values separated by dots.
     *
     * @param version
     *            a version string
     * @return whether version is a stable version
     */
    public static boolean isStableVersion(String version) {
        return version.matches("[0-9]+\\.[0-9]+\\.[0-9]+");
    }

    /**
     * Checks whether version1 and version2 represent the same version when
     * taking into account only the beginning parts of the version strings. It
     * is ignored whether the versions are snapshots. For instance,
     * isSameVersion("7.5.3", "7.5-SNAPSHOT", 2) returns true.
     *
     * The parameter digits must not be greater than the number of dot-separated
     * substrings in either version string.
     *
     * @param version1
     *            a version string
     * @param version2
     *            a version string
     * @param digits
     *            how many parts (separated by ".") are taken into account
     * @return whether version1 and version2 are the same up to the given number
     *         of "."-separated parts.
     */
    public static boolean isSameVersion(String version1, String version2,
            int digits) {
        version1 = version1.replaceAll("-.*", "");
        version2 = version2.replace("-.*", "");
        String[] v1parts = version1.split("\\.");
        String[] v2parts = version2.split("\\.");
        for (int i = 0; i < digits; i++) {
            if (!v1parts[i].equals(v2parts[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether version1 is newer or older than version2. The version
     * strings should consist of numeric values separated by dots, except that
     * the last substring may be nonnumeric, such as "alpha1" or "7.5-SNAPSHOT".
     *
     * The version strings should either have the same number of dot-separated
     * substrings or one should have one part more than the other, the extra
     * part indicating a prerelease such as "beta1".
     *
     * @param version1
     *            A version of the form a.b.c.
     * @param version2
     *            A version in the form x.y.z.w.
     * @return A positive number, zero or a negative number depending on whether
     *         version1 is newer than, as new as, or older than version2.
     */
    public static int compareVersions(String version1, String version2) {
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");
        int partCount = Math.min(parts1.length, parts2.length);
        for (int i = 0; i < partCount; i++) {
            int comparePart = compareVersionPart(parts1[i], parts2[i]);
            if (comparePart != 0) {
                return comparePart;
            }
        }
        // A stable version is newer than a prerelease, given that no other
        // difference was found.
        return (isStableVersion(version1) ? 1 : -1)
                - (isStableVersion(version2) ? 1 : -1);
    }

    private static int compareVersionPart(String part1, String part2) {
        // A snapshot is considered older than a numeric version if the
        // version is otherwise the same.
        boolean isSnapShot1 = part1.endsWith("SNAPSHOT");
        if (isSnapShot1) {
            part1 = part1.substring(0, part1.length() - 9);
        }
        boolean isSnapShot2 = part2.endsWith("SNAPSHOT");
        if (isSnapShot2) {
            part2 = part2.substring(0, part2.length() - 9);
        }
        Integer version1Part = null, version2Part = null;
        try {
            version1Part = Integer.parseInt(part1);
        } catch (NumberFormatException e) {
        }
        try {
            version2Part = Integer.parseInt(part2);
        } catch (NumberFormatException e) {
        }
        if (version1Part != null && version2Part != null) {
            // Return a positive number if version1Part >
            // version2Part or the versions are otherwise the same but part2
            // is a snapshot.
            return 2 * (version1Part - version2Part)
                    + ((isSnapShot2 ? 1 : 0) - (isSnapShot1 ? 1 : 0));
        } else if (version1Part != null) {
            // Part1 was numeric but part2 not, part2 is probably a prerelease.
            return 1;
        } else if (version2Part != null) {
            // Part2 was numeric but part1 not, part1 is probably a prerelease.
            return -1;
        } else {
            // Both versions were nonnumeric such as alpha, beta. Use string
            // comparison.
            return part1.compareTo(part2);
        }
    }

    private static boolean isAtLeastVersionString(String vaadinVersion,
            int majorVersion, int minorVersion) {
        if (null == vaadinVersion) {
            return false;
        }
        String[] versionStrings = vaadinVersion.split("[.-]");
        if (versionStrings.length < 2) {
            return false;
        }
        try {
            int major = Integer.parseInt(versionStrings[0]);
            int minor = Integer.parseInt(versionStrings[1]);
            return major > majorVersion
                    || (major == majorVersion && minor >= minorVersion);
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
