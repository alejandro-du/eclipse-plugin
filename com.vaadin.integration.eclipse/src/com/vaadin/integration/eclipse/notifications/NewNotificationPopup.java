package com.vaadin.integration.eclipse.notifications;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

import com.vaadin.integration.eclipse.notifications.model.Notification;

/**
 * Shows new single notification with its details.
 * 
 * @author denis
 *
 */
class NewNotificationPopup extends AbstractNotificationPopup {

    private final Notification notification;

    NewNotificationPopup(Notification notification) {
        this.notification = notification;
    }

    @Override
    protected String getPopupShellTitle() {
        return Messages.Notifications_PopupNotificationTitle;
    }

    @Override
    protected void createContentArea(Composite parent) {
        super.createContentArea(parent);

        SingleNotificationComposite control = new SingleNotificationComposite(
                parent, notification, getManager());
        GridDataFactory.fillDefaults().grab(true, true).span(2, 1)
                .align(SWT.FILL, SWT.FILL).applyTo(control);
        GridData data = (GridData) control.getLayoutData();
        data.heightHint = Utils.ITEM_HEIGHT;
    }

}
