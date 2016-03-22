package com.vaadin.integration.eclipse.preferences;

/**
 * Constant definitions for plug-in preferences
 */
public class PreferenceConstants {

    /*
     * =========================================================================
     * Notifications settings
     */

    public static final String NOTIFICATIONS_USER_TOKEN = "notificationsSignedInUserTokenPreference";

    public static final String NOTIFICATIONS_CENTER_POLLING_INTERVAL = "notificationsCenterPollingIntervalPreference";

    public static final String NOTIFICATIONS_NEW_VERSION_POLLING_INTERVAL = "notificationsNewVersionPollingIntervalPreference";

    public static final String NOTIFICATIONS_ENABLED = "notificationsEnabledPreference";

    public static final String NOTIFICATIONS_CENTER_POPUP_ENABLED = "notificationsCenterPopupEnabledPreference";

    public static final String NOTIFICATIONS_NEW_VERSION_POPUP_ENABLED = "notificationsNewVersionPopupEnabledPreference";

    public static final String NOTIFICATIONS_FETCH_ON_START = "notificationsFetchOnStartPreference";

    public static final String NOTIFICATIONS_FETCH_ON_OPEN = "notificationsFetchOnOpenPreference";

    /**
     * List of notification ids that are read for anonymous user (without
     * token).
     */
    public static final String NOTIFICATIONS_READ_IDS = "notificationsAnonymousReadIdsPreference";

    /**
     * This has a boolean value: should the version notification item (in the
     * popup list) be shown or not. The version notification item has special
     * meaning: it should not be shown if user has seen version notification and
     * decided to dismiss them (item is just informational and there is no need
     * to keep it always in this list to avoid a mess). "Dismiss" action is not
     * available for "regular" notifications. It's only possible to make them
     * read. But the "regular" notifications list (and its visibility) is
     * maintained by the notification center server.
     *
     * This has no relation to showing "new versions" notification popup
     * settings (which is not a list of all available notification but the
     * dedicated notification triggered by the background job). Settings for
     * this kind of popups are generic (f.e. NOTIFICATIONS_ENABLED
     * enables/disables notification functionality overall).
     */
    public static final String NOTIFICATIONS_VERSION_UPDATE_ITEM = "notificationsVersionUpdatePreference";

    public static final String NOTIFICATIONS_VERSIONS_INFO_READ = "notificationsVersionsInfoReadPreference";

    public static final String NOTIFICATIONS_SETTINGS_URL = "notificationsSettingsUrlPreference";

    /*
     * =========================================================================
     * Pre-release settings
     */

    public static final String PRERELEASE_ARCHETYPES_ENABLED = "prereleaseArchetypesEnabledPreference";

    /*
     * =========================================================================
     * Maven settings
     */

    /**
     * True to compile the widgetset on any relevant change.
     */
    public static final String MAVEN_WIDGETSET_AUTOMATIC_BUILD_ENABLED = "mavenAutomaticWidgetsetBuildPreference";

}
