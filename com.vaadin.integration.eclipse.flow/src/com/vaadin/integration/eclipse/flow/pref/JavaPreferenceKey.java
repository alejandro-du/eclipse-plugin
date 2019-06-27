package com.vaadin.integration.eclipse.flow.pref;

/**
 * Enumeration of preference keys used
 *
 * @see JavaPreferenceHandler
 */
public enum JavaPreferenceKey {
    /**
     * Key for saving the unique identifier for this user
     */
    ID("id", JavaPreferenceHandler.DESIGNER_PREFERENCES_PATH), INSTALLED(
            "installed", JavaPreferenceHandler.PLUGIN_PREFERENCES_PATH);

    private final String keyString;
    private final String prefPath;

    JavaPreferenceKey(String keyString, String prefPath) {
        this.keyString = keyString;
        this.prefPath = prefPath;
    }

    /**
     * @return the string representation of this key
     */
    String getKeyString() {
        return keyString;
    }

    String getPrefPath() {
        return prefPath;
    }
}
