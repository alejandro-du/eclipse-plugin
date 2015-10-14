package com.vaadin.integration.eclipse.notifications.model;

import org.eclipse.swt.graphics.Image;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.notifications.Utils;

public class SignInNotification extends Notification {

    public SignInNotification() {
        super();
    }

    @Override
    public Image getIcon() {
        return VaadinPlugin.getInstance().getImageRegistry()
                .get(Utils.SIGN_IN_ICON);
    }

}
