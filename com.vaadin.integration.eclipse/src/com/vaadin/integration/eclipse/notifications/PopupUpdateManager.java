package com.vaadin.integration.eclipse.notifications;

import com.vaadin.integration.eclipse.notifications.model.Notification;

/**
 * Manages update changes in the notifications list popup.
 *
 */
interface PopupUpdateManager {

    /**
     * Shows Sign In view.
     */
    void showSignIn();

    /**
     * Show recent notifications list (request its latest state and show).
     */
    void showNotificationsList();

    /**
     * Reveal all notifications and show them in the list.
     * <p>
     * Normally there is a limit of notifications to show because long list can
     * cause performance issues in current approach of item creation. This
     * method requests to show all of them without limit.
     */
    void revealAllNotifications();

    /**
     * Show provided {@code notification} (navigate to notification info view).
     */
    void showNotification(Notification notification);

    /**
     * Show token input view.
     */
    void showTokenInput();

    /**
     * Close the popup.
     */
    void close();

    /**
     * Remove version notification from the list and make notifications list
     * active.
     * 
     * Version notification will be hidden until next round of version checks
     * (will reappear if new versions are discovered comparing to previous
     * round).
     */
    void dismissVersionNotification();

}
