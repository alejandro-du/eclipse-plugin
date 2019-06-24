package com.vaadin.integration.eclipse.flow.pref;

import java.util.Objects;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Helper class for reading and writing Java preferences
 *
 * @see Preferences
 */
public class JavaPreferenceHandler {

    public static final String DESIGNER_PREFERENCES_PATH = "com/vaadin/designer";
    public static final String PLUGIN_PREFERENCES_PATH = "com/vaadin/eclipse/plugin";

    private JavaPreferenceHandler() {
        // only static helper methods
    }

    /**
     * Check if the given preference value exists
     *
     * @param nodeKey
     *            the key to check
     * @return true if a value exists, false if not or an error occurred
     */
    public static boolean valueExists(JavaPreferenceKey nodeKey) {
        try {
            for (String key : Preferences.userRoot().node(nodeKey.getPrefPath())
                    .keys()) {
                if (Objects.equals(nodeKey.getKeyString(), key)) {
                    return true;
                }
            }
            return false;
        } catch (BackingStoreException e) {
            return false;
        }
    }

    /**
     * Save a boolean preference value
     *
     * @param nodeKey
     *            the key to save with
     * @param nodeValue
     *            the non-null value
     */
    public static void saveBooleanValue(JavaPreferenceKey nodeKey,
            boolean nodeValue) {
        Preferences.userRoot().node(nodeKey.getPrefPath())
                .putBoolean(nodeKey.getKeyString(), nodeValue);
    }

    /**
     * Save a String preference value
     *
     * @param nodeKey
     *            the key to save with
     * @param nodeValue
     *            the non-null value
     */
    public static void saveStringValue(JavaPreferenceKey nodeKey,
            String nodeValue) {
        Preferences.userRoot().node(nodeKey.getPrefPath())
                .put(nodeKey.getKeyString(), nodeValue);
    }

    /**
     * Retrieve a String preference value
     *
     * @param nodeKey
     *            the key retrieve
     * @return the String value or ""
     * @see #valueExists(JavaPreferenceKey)
     */
    public static String getStringValue(JavaPreferenceKey nodeKey) {
        return Preferences.userRoot().node(nodeKey.getPrefPath())
                .get(nodeKey.getKeyString(), "");
    }

    /**
     * Retrieve a boolean preference value
     *
     * @param nodeKey
     *            the key to retrieve
     * @return the boolean value or false
     * @see #valueExists(JavaPreferenceKey)
     */
    public static boolean getBooleanValue(JavaPreferenceKey nodeKey) {
        return Preferences.userRoot().node(nodeKey.getPrefPath())
                .getBoolean(nodeKey.getKeyString(), false);
    }
}
