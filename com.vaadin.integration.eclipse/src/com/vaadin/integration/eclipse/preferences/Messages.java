package com.vaadin.integration.eclipse.preferences;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = Messages.class.getPackage()
            .getName() + ".messages"; //$NON-NLS-1$

    public static String VaadinPreferences_NotificationsEnable;
    public static String VaadinPreferences_NotificationsFetchOnStart;
    public static String VaadinPreferences_NotificationsPopup;
    public static String VaadinPreferences_NotificationsSectionTitle;
    public static String VaadinPreferences_Never;
    public static String VaadinPreferences_NotificationsPollingInterval;
    public static String VaadinPreferences_OncePer4Hours;
    public static String VaadinPreferences_OncePerDay;
    public static String VaadinPreferences_OncePerHour;
    public static String VaadinPreferences_NotificationsFetchOnOpen;
    public static String VaadinPreferences_UpdateSchedule;
    public static String VaadinPreferences_NotificationsVersionPollingInterval;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
