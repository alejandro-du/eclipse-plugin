package com.vaadin.integration.eclipse.notifications.jobs.nightly;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = Messages.class.getPackage()
            .getName() + ".messages"; //$NON-NLS-1$
    public static String Notifications_NightlyCheckJobName;
    public static String Notifications_NightlySchedulerJobName;
    public static String Notifications_UsageStatJobName;
    public static String Notifications_UsageStatTask;
    public static String Notifications_UsageStatTaskName;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
