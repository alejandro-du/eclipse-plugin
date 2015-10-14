package com.vaadin.integration.eclipse.notifications;

import java.util.Collection;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

import com.vaadin.integration.eclipse.notifications.model.Notification;

/**
 * Shows new notifications informational popup (without exact notification
 * content).
 * 
 * @author denis
 *
 */
class NewNotificationsPopup extends AbstractNotificationPopup {

    private final Collection<Notification> notifications;

    NewNotificationsPopup(Collection<Notification> notifications) {
        this.notifications = notifications;
    }

    @Override
    protected void createContentArea(Composite parent) {
        super.createContentArea(parent);

        FewNotificationsComposite control = new FewNotificationsComposite(
                parent, getManager(), notifications);
        GridDataFactory.fillDefaults().grab(true, true).span(2, 1)
                .align(SWT.FILL, SWT.FILL).applyTo(control);
        GridData data = (GridData) control.getLayoutData();
        data.heightHint = Utils.ITEM_HEIGHT;
    }
}
