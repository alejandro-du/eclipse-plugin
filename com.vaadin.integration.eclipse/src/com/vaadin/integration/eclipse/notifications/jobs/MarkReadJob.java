package com.vaadin.integration.eclipse.notifications.jobs;

import java.util.Collections;
import java.util.List;

import com.vaadin.integration.eclipse.notifications.model.NotificationsService;

/**
 * A background job to mark notifications as read (or skipped) on the server.
 */
public class MarkReadJob extends AbstractNotificationHandleJob {

    private final List<String> ids;

    public MarkReadJob(String token, List<String> ids) {
        super(Messages.Notifications_MarkReadJobName, token,
                ids.isEmpty() ? null : ids.get(0));
        this.ids = Collections.unmodifiableList(ids);
    }

    public MarkReadJob(String token, String notificationId) {
        this(token, Collections.singletonList(notificationId));
    }

    @Override
    protected void handleNotification(String token, String id) {
        if (ids.size() > 1) {
            NotificationsService.getInstance().skipNotifications(token, ids);
        } else if (!ids.isEmpty()) {
            NotificationsService.getInstance().markRead(token, ids.get(0));
        }
    }

    @Override
    protected String getTaskName() {
        return Messages.Notifications_MarkReadTask;
    }
}
