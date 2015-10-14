package com.vaadin.integration.eclipse.notifications.jobs;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.vaadin.integration.eclipse.notifications.Consumer;
import com.vaadin.integration.eclipse.notifications.Pair;
import com.vaadin.integration.eclipse.notifications.model.Notification;
import com.vaadin.integration.eclipse.notifications.model.NotificationsService;

/**
 * A job to fetch (user specific or generic) notifications and the related
 * icons/images from the server or the local cache.
 *
 * This job is used for notification fetching triggered by UI start and explicit
 * user actions - for the periodic background updates, see
 * {@link NewNotificationsJob} and its custom version of FetchNotificationJob
 * instead.
 */
public class FetchNotificationsJob extends
        AbstractNotificationJob<Pair<String, Collection<Notification>>> {

    private final String token;
    private final boolean useCached;

    public FetchNotificationsJob(
            Consumer<Pair<String, Collection<Notification>>> consumer,
            String token, boolean useCached) {
        this(Messages.Notifications_FetchJobName, consumer, token, useCached);
    }

    protected FetchNotificationsJob(String name,
            Consumer<Pair<String, Collection<Notification>>> consumer,
            String token, boolean useCached) {
        super(name, consumer);
        setUser(false);

        this.token = token;
        this.useCached = useCached;

        setRule(NotificationsUpdateRule.getInstance());
    }

    protected String getTaskName() {
        return Messages.Notifications_FetchTask;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        monitor.beginTask(getTaskName(), 3);
        try {
            Collection<Notification> notifications;
            if (useCached) {
                notifications = Collections
                        .unmodifiableCollection(NotificationsService
                                .getInstance().getCachedNotifications(token));
            } else {
                notifications = Collections
                        .unmodifiableCollection(NotificationsService
                                .getInstance().getAllNotifications(token));
            }

            monitor.worked(1);
            if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            NotificationsService.getInstance().downloadIcons(notifications);
            monitor.worked(1);
            getConsumer().accept(new Pair<String, Collection<Notification>>(
                    token, Collections.unmodifiableCollection(notifications)));
            if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }

            NotificationsService.getInstance().downloadImages(notifications);
            monitor.worked(1);
        } finally {
            monitor.done();
        }

        return Status.OK_STATUS;
    }

}
