package com.vaadin.integration.eclipse.notifications;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Control;

import com.vaadin.integration.eclipse.notifications.ContributionService.ServiceMediator;
import com.vaadin.integration.eclipse.notifications.model.Notification;

/**
 * Consumer of a set of all notifications fetched from the server or the local
 * cache.
 *
 * This class notifies {@link ContributionService} of changes, handles some
 * read/unread state tagging and updates the UI (popup and the notification
 * list).
 */
final class AllNotificationsConsumer
        extends AbstractConsumer<Pair<String, Collection<Notification>>> {

    private final boolean reset;
    private final ServiceMediator mediator;

    AllNotificationsConsumer(ServiceMediator mediator, boolean reset) {
        this.mediator = mediator;
        this.reset = reset;
    }

    @Override
    protected void handleData(Pair<String, Collection<Notification>> data) {
        if (!isTokenValid(data.getFirst())) {
            // Do not do anything if other job has already changed the token
            return;
        }
        if (reset) {
            mediator.setNotifications(Collections.<Notification> emptyList());
        }
        Map<String, Notification> map;
        if (!ContributionService.getInstance().getNotifications().isEmpty()) {
            map = new HashMap<String, Notification>();
            for (Notification notification : ContributionService.getInstance()
                    .getNotifications()) {
                map.put(notification.getId(), notification);
            }
        } else {
            map = Collections.emptyMap();
        }
        List<Notification> newNotifications = new ArrayList<Notification>();
        List<String> anonymouslyReadIds = data.getFirst() == null
                ? mediator.getAnonymouslyReadIds() : null;
        for (Notification notification : data.getSecond()) {
            if (data.getFirst() == null
                    && anonymouslyReadIds.contains(notification.getId())) {
                notification.setRead();
            }

            Notification known = map.get(notification.getId());
            if (known == null) {
                newNotifications.add(notification);
            } else if (known.isRead() && !notification.isRead()) {
                // in case notification has been updated recently it
                // should be shown in popup
                newNotifications.add(notification);
            }
        }
        if (!reset && !newNotifications.isEmpty()) {
            informNewNotifications(newNotifications);
        }
        mediator.setNotifications(data.getSecond());
        ContributionService.getInstance().updateContributionControl();
    }

    private void informNewNotifications(Collection<Notification> collection) {
        Control control = ContributionService.getInstance()
                .getContributionControl();
        if (control == null || control.isDisposed()) {
            return;
        }
        if (isPopupEnabled()) {
            if (collection.size() == 1) {
                NewNotificationPopup popup = new NewNotificationPopup(
                        collection.iterator().next());
                popup.open();
            } else if (!collection.isEmpty()) {
                NewNotificationsPopup popup = new NewNotificationsPopup(
                        collection);
                popup.open();
            }
        }
    }

    private boolean isPopupEnabled() {
        return ContributionService.getInstance()
                .isNotificationsCenterPopupEnabled();
    }

    private boolean isTokenValid(String token) {
        if (token == null) {
            return ContributionService.getInstance().getToken() == null;
        }
        return token.equals(ContributionService.getInstance().getToken());
    }

}