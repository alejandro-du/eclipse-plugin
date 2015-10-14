package com.vaadin.integration.eclipse.notifications;

import org.eclipse.swt.widgets.Composite;

import com.vaadin.integration.eclipse.notifications.model.Notification;

/**
 * Notification item to show it in the list of notifications.
 *
 */
class NotificationItem extends AbstractNotificationItem implements ItemAction {

    NotificationItem(Composite parent, Notification notification,
            ItemStyle style) {
        super(parent, notification, style);
    }

    public void runAction(PopupUpdateManager manager) {
        activate();
        manager.showNotification(getNotification());
    }

    @Override
    protected void activate() {
        super.activate();
        ContributionService.getInstance().markRead(getNotification());
    }

}
