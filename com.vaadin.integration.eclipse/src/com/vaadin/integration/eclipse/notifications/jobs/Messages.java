package com.vaadin.integration.eclipse.notifications.jobs;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = Messages.class.getPackage()
            .getName() + ".messages"; //$NON-NLS-1$
    public static String Notifications_FetchJobName;
    public static String Notifications_FetchNewJob;
    public static String Notifications_FetchNewTask;
    public static String Notifications_FetchTask;
    public static String Notifications_MarkReadJobName;
    public static String Notifications_MarkReadTask;
    public static String Notifications_SignInJob;
    public static String Notifications_SignInTask;
    public static String Notifications_StatisticsJobName;
    public static String Notifications_StatisticsTask;
    public static String Notifications_ValidationJobName;
    public static String Notifications_ValidationTask;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
