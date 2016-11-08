package com.vaadin.integration.eclipse.notifications;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = Messages.class.getPackage()
            .getName() + ".messages"; //$NON-NLS-1$

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }

    public static String Notifications_BackAction;
    public static String Notifications_ClearAll;
    public static String Notifications_NotificationInfoReadMore;
    public static String Notifications_PopupNotificationsTitle;
    public static String Notifications_PopupNotificationTitle;
    public static String Notifications_ReadMore;
    public static String Notifications_Settings;
    public static String Notifications_SeveralNotificationsMsgParameter;
    public static String Notifications_SeveralNotificationsMessage;
    public static String Notifications_SignIn;
    public static String Notifications_SignInEmail;
    public static String Notifications_SignInError;
    public static String Notifications_SignInItemSeeYourNotifications;
    public static String Notifications_SignInItemUseAccount;
    public static String Notifications_SignInPassword;
    public static String Notifications_SignInWithSuffix;
    public static String Notifications_SignOut;
    public static String Notifications_TokenDescriptionItem1;
    public static String Notifications_TokenDescriptionItem2;
    public static String Notifications_TokenDescriptionItem3;
    public static String Notifications_TokenDescriptionItem4;
    public static String Notifications_TokenDescriptionVaadin;
    public static String Notifications_TokenItemNumber;
    public static String Notifications_waitingFocus;
    public static String Notifications_TokenViewTitle;
    public static String Notifications_TokenErrorMsg;
    public static String Notifications_VersionsDismiss;
    public static String Notifications_VersionsTitle;
    public static String Notifications_VersionsVaadin7AvailableVersions;
    public static String Notifications_VersionsVaadin7ProjectItem;
    public static String Notifications_VersionsVaadin7ProjectsTitle;
    public static String Notifications_VersionUpgradeItemDescr;
    public static String Notifications_VersionUpgradeItemSummary;
    public static String Notifications_ShowMoreNotifications;
    public static String Notifications_MoreNotificationsAvailable;
    public static String Notifications_SignInProgress;
    public static String Notifications_BrowserFailTitle;
    public static String Notifications_SettingsFailMsg;
    public static String Notifications_BrowserFailReason;
    public static String Notifications_TokenUrlFailMsg;
    public static String Notifications_NewVersionsPopup;
    public static String Notifications_NewVersionsPopupMore;
}
