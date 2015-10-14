package com.vaadin.integration.eclipse.notifications;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import com.vaadin.integration.eclipse.notifications.model.SignInNotification;

class SignInItem extends AbstractNotificationItem implements ItemAction {

    SignInItem(Composite parent, SignInNotification notification,
            ItemStyle style) {
        super(parent, notification, style);
    }

    @Override
    protected Control createInfoSection() {
        Composite composite = new Composite(this, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        composite.setLayout(layout);

        Label label = new Label(composite, SWT.NONE);
        label.setText(getSummary());
        label.setFont(getItemFont());
        label.setForeground(getItemTextColor());

        buildShortDecriptionText(composite);

        return composite;
    }

    public void runAction(PopupUpdateManager manager) {
        activate();
        manager.showSignIn();
    }

    @Override
    protected String getSummary() {
        return Messages.Notifications_SignInItemUseAccount;
    }

    @Override
    protected Control buildShortDecriptionText(Composite composite) {
        Label label = new Label(composite, SWT.NONE);
        label.setText(Messages.Notifications_SignInItemSeeYourNotifications);
        label.setFont(getItemFont());
        label.setForeground(getItemTextColor());
        return label;
    }
}
