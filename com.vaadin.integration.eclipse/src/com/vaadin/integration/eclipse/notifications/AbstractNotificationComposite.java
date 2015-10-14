package com.vaadin.integration.eclipse.notifications;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import com.vaadin.integration.eclipse.notifications.model.Notification;

abstract class AbstractNotificationComposite extends AbstractNotificationItem {

    private final PopupManager manager;
    private ClickListener listener;

    AbstractNotificationComposite(Composite parent, Notification notification,
            PopupManager manager) {
        this(parent, notification, manager, true);
    }

    protected AbstractNotificationComposite(Composite parent,
            Notification notification, PopupManager manager,
            boolean hasNewIndicator) {
        super(parent, notification, hasNewIndicator);
        this.manager = manager;

        listener = new ClickListener();
        parent.getDisplay().addFilter(SWT.MouseDown, listener);
        addDisposeListener(listener);
    }

    protected abstract void showDetails();

    protected PopupManager getManager() {
        return manager;
    }

    private class ClickListener implements DisposeListener, Listener {

        public void widgetDisposed(DisposeEvent e) {
            getParent().getDisplay().removeFilter(SWT.MouseDown, this);
        }

        public void handleEvent(Event event) {
            if (Utils.isControlClicked(event,
                    AbstractNotificationComposite.this)) {
                showDetails();
            }
        }
    }

}
