package com.vaadin.integration.eclipse.notifications;

import org.eclipse.mylyn.commons.workbench.forms.ScalingHyperlink;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;

class NotificationHyperlink extends ScalingHyperlink {

    NotificationHyperlink(Composite parent) {
        super(parent, SWT.LEFT | SWT.NO_FOCUS);
    }

    NotificationHyperlink(Composite parent, int style) {
        super(parent, style | SWT.NO_FOCUS);
    }

    @Override
    public void addListener(int eventType, Listener listener) {
        if (eventType == SWT.FocusIn || eventType == SWT.FocusOut) {
            return;
        }
        super.addListener(eventType, listener);
    }
}
