package com.vaadin.integration.eclipse.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.util.data.AbstractVaadinVersion;
import com.vaadin.integration.eclipse.util.data.MavenVaadinVersion;
import com.vaadin.integration.eclipse.util.files.LocalFileManager.FileType;

/**
 * Helper class for accessing per-project preferences of the Vaadin plug-in.
 */
public class PreferenceUtil {

    private ScopedPreferenceStore prefStore;

    private PreferenceUtil(IProject project) {
        prefStore = new ScopedPreferenceStore(new ProjectScope(project),
                VaadinPlugin.PLUGIN_ID);
    }

    // preference store keys

    public static PreferenceUtil get(IProject project) {
        return new PreferenceUtil(project);
    }

    /*
     * "Global" pref keys, referenced from elsewhere. (Project scoped keys
     * follow later.)
     */

    // true when Ivy resolving info popup is disabled
    public static final String PREFERENCES_IVYINFO_DISABLED = VaadinPlugin.PLUGIN_ID
            + "." + "IvyInformationDisabled";

    // true when Eclipse bug info popup is disabled
    public static final String PREFERENCES_ECLIPSE_LUNA_SR1_BUG_INFO = VaadinPlugin.PLUGIN_ID
            + "." + "Eclipse44BugInformationDisabled";

    /*
     * Project scoped keys
     */

    // "true"/"false"/missing - if missing, check if >1 widgetset exists
    private static final String PREFERENCES_WIDGETSET_DIRTY = VaadinPlugin.PLUGIN_ID
            + "." + "widgetsetDirty";
    // true to suspend automatic widgetset build requests for the project
    private static final String PREFERENCES_WIDGETSET_SUSPENDED = VaadinPlugin.PLUGIN_ID
            + "." + "widgetsetBuildsSuspended";
    // "OBF"/"PRETTY"/"DETAILED" or missing (default to "OBF")
    private static final String PREFERENCES_WIDGETSET_STYLE = VaadinPlugin.PLUGIN_ID
            + "." + "widgetsetStyle";
    // a number of threads to use (-localWorkers) or missing
    private static final String PREFERENCES_WIDGETSET_PARALLELISM = VaadinPlugin.PLUGIN_ID
            + "." + "widgetsetParallelism";

    // the time last compilation lasted, used for estimation in progress monitor
    private static final String PREFERENCES_WIDGETSET_COMPILATION_ETA = VaadinPlugin.PLUGIN_ID
            + "." + "widgetsetCompilationEta";

    // to output compilation messages to console or not
    private static final String PREFERENCES_WIDGETSET_VERBOSE = VaadinPlugin.PLUGIN_ID
            + "." + "widgetsetVerbose";

    // extra parameters for widgetset compilation
    private static final String PREFERENCES_WIDGETSET_EXTRA_PARAMETERS = VaadinPlugin.PLUGIN_ID
            + "." + "widgetsetExtraParameters";

    // project type flags - note that in the future, there could be multiple
    // flags set at the same time
    private static final String PREFERENCES_PROJECT_TYPE_GAE = VaadinPlugin.PLUGIN_ID
            + "." + "projectTypeGae";

    // "true"/"false"/missing - missing means false
    private static final String PREFERENCES_USE_LATEST_NIGHTLY = VaadinPlugin.PLUGIN_ID
            + "." + "useLatestNightly";

    // "true"/"false"/missing - if missing, default value is taken from
    // Eclipse preferences
    private static final String PREFERENCES_UPDATE_NOTIFICATION_ENABLED = VaadinPlugin.PLUGIN_ID
            + ".notifyOfVaadinUpdates";

    // true to suspend scanning for addon themes and creating the addons.scss
    // file
    private static final String PREFERENCES_ADDON_THEME_SCANNING_SUSPENDED = VaadinPlugin.PLUGIN_ID
            + "." + "addonThemesSuspended";

    // true to suspend automatic compilation of themes
    private static final String PREFERENCES_THEME_COMPILATION_SUSPENDED = VaadinPlugin.PLUGIN_ID
            + "." + "themeCompilationSuspended";

    // VaadinPlugin.COMPILE_ACTION_* or missing - if missing, defaults to
    // widgetset
    private static final String PREFERENCES_PREVIOUS_COMPILE_ACTION = VaadinPlugin.PLUGIN_ID
            + "." + "previousCompileAction";

    // Latest nightly version suggested for the project to upgrade (and may be
    // already upgraded). Required for persisted notification about versions
    // upgrade.
    private static final String PREFERENCES_NIGHTLY_LATEST_VERSION_UPGRADE = VaadinPlugin.PLUGIN_ID
            + "." + "nightlyLatestVersionUpgrade";

    // Accompanied with previous key. Contains file type for the version
    private static final String PREFERENCES_NIGHTLY_LATEST_VERSION_FTYPE = VaadinPlugin.PLUGIN_ID
            + "." + "nightlyLatestVersionFileType";

    // Latest maven versions suggested for the project to upgrade. Required for
    // persisted notification about version upgrade.
    private static final String PREFERENCES_MAVEN_LATEST_VERSIONS_UPGRADE = VaadinPlugin.PLUGIN_ID
            + "." + "mavenLatestVersionsUpgrade";

    // three-value setting for per-project widgetset autocompilation
    // ("null"/"true"/"false")
    private static final String PREFERENCES_MAVEN_AUTO_COMPILE_WIDGETSET = VaadinPlugin.PLUGIN_ID
            + "." + "mavenAutoCompileWidgetset";

    /**
     * Checks whether scanning for addon themes has explicitly been suspended by
     * the user
     */
    public boolean isAddonThemeScanningSuspended() {
        if (!prefStore.contains(PREFERENCES_ADDON_THEME_SCANNING_SUSPENDED)) {
            return false;
        } else {
            return prefStore
                    .getBoolean(PREFERENCES_ADDON_THEME_SCANNING_SUSPENDED);
        }
    }

    /**
     * Suspends the addon theme scanning and so stops updating the addons.scss
     * file with further changes
     * 
     * @param suspended
     *            true if no updates to the addons.scss should be made
     *            automatically
     */
    public void setAddonThemeScanningSuspended(boolean suspended) {
        prefStore.setValue(PREFERENCES_ADDON_THEME_SCANNING_SUSPENDED,
                suspended);
    }

    /**
     * Checks whether automatic compilation of themes has explicitly been
     * suspended by the user
     */
    public boolean isThemeCompilationSuspended() {
        if (!prefStore.contains(PREFERENCES_THEME_COMPILATION_SUSPENDED)) {
            return false;
        } else {
            return prefStore
                    .getBoolean(PREFERENCES_THEME_COMPILATION_SUSPENDED);
        }
    }

    /**
     * Suspends automatic theme compilation
     * 
     * @param suspended
     *            true if themes should not be compiled automatically
     */
    public void setThemeCompilationSuspended(boolean suspended) {
        prefStore.setValue(PREFERENCES_THEME_COMPILATION_SUSPENDED, suspended);
    }

    /**
     * Checks whether widgetset building for a project has been suspended
     * explicitly by the user.
     * 
     * @param project
     * @return
     */
    public boolean isWidgetsetCompilationSuspended() {
        if (!prefStore.contains(PREFERENCES_WIDGETSET_SUSPENDED)) {
            return true;
        } else {
            return prefStore.getBoolean(PREFERENCES_WIDGETSET_SUSPENDED);
        }
    }

    /**
     * Sets the suspended flag for widgetset compilation. If suspended the
     * widgetset will not be compiled automatically by the plugin. Returns true
     * if the value was changed, false if it remained the same.
     * 
     * @param parallelism
     * @return
     */
    public boolean setWidgetsetCompilationSuspended(boolean suspended) {
        boolean oldValue = isWidgetsetCompilationSuspended();
        // need to store as String because default value is "true"
        prefStore.setValue(PREFERENCES_WIDGETSET_SUSPENDED,
                Boolean.toString(suspended));
        return oldValue != suspended;
    }

    public boolean isWidgetsetCompilationVerboseMode() {
        if (!prefStore.contains(PREFERENCES_WIDGETSET_VERBOSE)) {
            return true;
        } else {
            return prefStore.getBoolean(PREFERENCES_WIDGETSET_VERBOSE);
        }
    }

    /**
     * Sets the verbosity mode used in widgetset compilation. Returns true if
     * the value was changed, false if it remained the same.
     * 
     * @param parallelism
     * @return
     */
    public boolean setWidgetsetCompilationVerboseMode(boolean verbose) {
        boolean oldValue = isWidgetsetCompilationVerboseMode();
        // need to store as String because default value is "true"
        prefStore.setValue(PREFERENCES_WIDGETSET_VERBOSE,
                Boolean.toString(verbose));
        return oldValue != verbose;

    }

    public String getWidgetsetCompilationStyle() {
        if (!prefStore.contains(PREFERENCES_WIDGETSET_STYLE)) {
            return "OBF";
        } else {
            return prefStore.getString(PREFERENCES_WIDGETSET_STYLE);
        }
    }

    /**
     * Sets the style parameter used in widgetset compilation. Returns true if
     * the value was changed, false if it remained the same.
     * 
     * @param style
     * @return
     */
    public boolean setWidgetsetCompilationStyle(String style) {
        String oldValue = getWidgetsetCompilationStyle();
        if (style == null || "".equals(style)) {
            // Convert "" -> "OBF" to be more explicit
            style = "OBF";
        }
        if (oldValue == null || "".equals(oldValue)) {
            // Convert ""-> "OBF" for backwards compatibility.
            oldValue = "OBF";
        }
        prefStore.setValue(PREFERENCES_WIDGETSET_STYLE, style);
        return !equals(oldValue, style);

    }

    public String getWidgetsetCompilationExtraParameters() {
        return prefStore.getString(PREFERENCES_WIDGETSET_EXTRA_PARAMETERS);
    }

    /**
     * Sets the extra command line parameters used in widgetset compilation.
     * Returns true if the value was changed, false if it remained the same.
     * 
     * @param params
     * @return
     */
    public boolean setWidgetsetCompilationExtraParameters(String params) {
        String oldValue = prefStore
                .getString(PREFERENCES_WIDGETSET_EXTRA_PARAMETERS);
        if (params == null) {
            params = "";
        }
        prefStore.setValue(PREFERENCES_WIDGETSET_EXTRA_PARAMETERS, params);
        return !equals(oldValue, params);

    }

    public String getWidgetsetCompilationParallelism() {
        if (!prefStore.contains(PREFERENCES_WIDGETSET_PARALLELISM)) {
            return "";
        } else {
            return prefStore.getString(PREFERENCES_WIDGETSET_PARALLELISM);
        }
    }

    /**
     * Sets the parallelism parameter used in widgetset compilation. Returns
     * true if the value was changed, false if it remained the same.
     * 
     * @param parallelism
     * @return
     */
    public boolean setWidgetsetCompilationParallelism(String parallelism) {
        String oldValue = getWidgetsetCompilationParallelism();
        prefStore.setValue(PREFERENCES_WIDGETSET_PARALLELISM, parallelism);
        return !equals(oldValue, parallelism);
    }

    /**
     * Persist latest suggested (and may be upgraded) version for the project.
     */
    public void setLatestNightlyUpgradeVersion(AbstractVaadinVersion version) {
        prefStore.setValue(PREFERENCES_NIGHTLY_LATEST_VERSION_UPGRADE,
                version.getVersionNumber());
        prefStore.setValue(PREFERENCES_NIGHTLY_LATEST_VERSION_FTYPE,
                version.getType().name());
    }

    /**
     * Persist latest suggested maven versions for the project.
     */
    public void setLatestMavenUpgradeVersions(
            List<MavenVaadinVersion> versions) {
        JSONArray array = new JSONArray();
        for (MavenVaadinVersion version : versions) {
            array.add(version.getVersionNumber());
        }
        prefStore.setValue(PREFERENCES_MAVEN_LATEST_VERSIONS_UPGRADE,
                array.toJSONString());
    }

    /**
     * Get persisted latest suggested (and may be upgraded) version for the
     * project.
     */
    public AbstractVaadinVersion getLatestNightlyUpgradeVersion() {
        String version = prefStore
                .getString(PREFERENCES_NIGHTLY_LATEST_VERSION_UPGRADE);
        String fType = prefStore
                .getString(PREFERENCES_NIGHTLY_LATEST_VERSION_FTYPE);
        if (version == null || version.isEmpty()) {
            return null;
        }
        return new AbstractVaadinVersion(version, fileTypeForName(fType)) {
        };
    }

    /**
     * Get persisted latest suggested maven versions for the project.
     */
    public List<MavenVaadinVersion> getLatestMavenUpgradeVersions() {
        String versions = prefStore
                .getString(PREFERENCES_MAVEN_LATEST_VERSIONS_UPGRADE);
        if (versions == null || versions.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            Object result = new JSONParser().parse(versions);
            if (result instanceof JSONArray) {
                List<MavenVaadinVersion> list = new ArrayList<MavenVaadinVersion>(
                        ((JSONArray) result).size());
                for (Object obj : (JSONArray) result) {
                    list.add(new MavenVaadinVersion(obj.toString()));
                }
                return list;
            }
        } catch (ParseException e) {
            Logger.getLogger(PreferenceUtil.class.getName()).log(Level.WARNING,
                    "Unable to parse persisted maven version "
                            + "which has been suggested as an upgrade",
                            e);
        }
        return Collections.emptyList();
    }

    private FileType fileTypeForName(String name) {
        // Don't use Enum.valueOf() since it throws an uncatched exception if
        // there is no enum value with given name.
        for (FileType type : FileType.values()) {
            if (name.equals(type.name())) {
                return type;
            }
        }
        return null;
    }

    /**
     * Compares the two values. Returns true if both are null or have the equal
     * value.
     * 
     * @param oldValue
     * @param newValue
     * @return
     */
    private boolean equals(Object oldValue, Object newValue) {
        if (oldValue == null) {
            return newValue == null;
        }
        if (newValue == null) {
            return false;
        }
        return oldValue.equals(newValue);
    }

    public void persist() throws IOException {
        prefStore.save();
    }

    public long getEstimatedCompilationTime() {
        if (prefStore.contains(PREFERENCES_WIDGETSET_COMPILATION_ETA)) {
            return prefStore.getLong(PREFERENCES_WIDGETSET_COMPILATION_ETA);
        } else {
            /**
             * Make an initial wild guess that compilation takes two minutes.
             */
            return 120 * 1000l;
        }

    }

    public void setWidgetsetCompilationTimeEstimate(long estimate) {
        prefStore.setValue(PREFERENCES_WIDGETSET_COMPILATION_ETA, estimate);

    }

    /**
     * Checks if the widgetset is marked as dirty. Returns true for dirty, false
     * for not dirty and null if no marking was found.
     * 
     * @return
     */
    public Boolean isWidgetsetDirty() {
        if (prefStore.contains(PREFERENCES_WIDGETSET_DIRTY)) {
            return prefStore.getBoolean(PREFERENCES_WIDGETSET_DIRTY);
        }

        return null;

    }

    public void setWidgetsetDirty(boolean dirty) {
        prefStore.setValue(PREFERENCES_WIDGETSET_DIRTY,
                Boolean.toString(dirty));
    }

    /**
     * Checks if the project is configured to use the latest nightly build.
     * 
     * @return
     */
    public boolean isUsingLatestNightly() {
        if (prefStore.contains(PREFERENCES_USE_LATEST_NIGHTLY)) {
            return prefStore.getBoolean(PREFERENCES_USE_LATEST_NIGHTLY);
        }
        return false;
    }

    /**
     * Sets whether the project should always use the latest nightly build of
     * the branch. Returns true if the value was changed, false if it remained
     * the same.
     * 
     * @param useLatestNightly
     * @return
     */
    public boolean setUsingLatestNightly(boolean useLatestNightly) {
        boolean oldValue = isUsingLatestNightly();
        prefStore.setValue(PREFERENCES_USE_LATEST_NIGHTLY,
                Boolean.toString(useLatestNightly));
        return oldValue != useLatestNightly;
    }

    /**
     * Checks whether the project is configured to show a notification when it
     * is possible to update to a newer Vaadin version.
     *
     * @return whether Vaadin version update notifications are enabled
     */
    public boolean isUpdateNotificationEnabled() {
        if (prefStore.contains(PREFERENCES_UPDATE_NOTIFICATION_ENABLED)) {
            return prefStore
                    .getBoolean(PREFERENCES_UPDATE_NOTIFICATION_ENABLED);
        }
        // If the project does not have this preference set, use the default
        return true;
    }

    /**
     * Sets whether a notification should be shown about possible Vaadin version
     * updates for the project.
     *
     * @param enableNotifications
     *            whether Vaadin update notifications are enabled
     */
    public void setUpdateNotificationEnabled(boolean enableNotifications) {
        prefStore.setValue(PREFERENCES_UPDATE_NOTIFICATION_ENABLED,
                Boolean.toString(enableNotifications));
    }

    /**
     * Sets a flag that marks if the project is a Google App Engine project.
     * 
     * @param gaeProject
     */
    public void setProjectTypeGae(boolean gaeProject) {
        prefStore.setValue(PREFERENCES_PROJECT_TYPE_GAE, gaeProject);
    }

    public boolean isGaeProject() {
        if (prefStore.contains(PREFERENCES_PROJECT_TYPE_GAE)) {
            return prefStore.getBoolean(PREFERENCES_PROJECT_TYPE_GAE);
        }

        return false;
    }

    /**
     * Returns the previously used compile action ("widgetset" or "theme" or
     * "both") - use the constants {@link VaadinPlugin}.COMPILE_ACTION_*.
     * 
     * @return previously used compile action, by default "widgetset" if none
     *         saved
     */
    public String getPreviousCompileAction() {
        if (!prefStore.contains(PREFERENCES_PREVIOUS_COMPILE_ACTION)) {
            return VaadinPlugin.COMPILE_ACTION_WIDGETSET;
        } else {
            return prefStore.getString(PREFERENCES_PREVIOUS_COMPILE_ACTION);
        }
    }

    /**
     * Sets the previously used compile action ("widgetset", "theme" or "both").
     * Returns true if the value was changed, false if it remained the same.
     * 
     * @param action
     *            either "widgetset", "theme" or "both" - use the constants
     *            {@link VaadinPlugin}.COMPILE_ACTION_*
     * @return true if the value was changed
     */
    public boolean setPreviousCompileAction(String action) {
        String oldValue = getPreviousCompileAction();
        prefStore.setValue(PREFERENCES_PREVIOUS_COMPILE_ACTION, action);
        return !equals(oldValue, action);
    }

    /**
     * Gets the project specific status of Maven widgetset auto-compilation.
     * 
     * @return true or false for per-project override or null to use the global
     *         default
     */
    public Boolean isMavenAutoCompileWidgetset() {
        if (!prefStore.contains(PREFERENCES_MAVEN_AUTO_COMPILE_WIDGETSET)
                || "null".equals(prefStore
                        .getString(PREFERENCES_MAVEN_AUTO_COMPILE_WIDGETSET))) {
            return null;
        } else {
            return prefStore
                    .getBoolean(PREFERENCES_MAVEN_AUTO_COMPILE_WIDGETSET);
        }
    }

    public boolean setMavenAutoCompileWidgetset(Boolean autoCompile) {
        Boolean oldValue = isMavenAutoCompileWidgetset();
        if (autoCompile != null) {
            prefStore.setValue(PREFERENCES_MAVEN_AUTO_COMPILE_WIDGETSET,
                    String.valueOf(autoCompile));
        } else {
            prefStore
                    .setValue(PREFERENCES_MAVEN_AUTO_COMPILE_WIDGETSET, "null");
        }
        return !equals(oldValue, autoCompile);
    }
}
