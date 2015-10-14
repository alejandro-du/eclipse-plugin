package com.vaadin.integration.eclipse.notifications;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.preferences.PreferenceConstants;

final class TokenConsumer extends AbstractConsumer<String> {

    private final Consumer<Boolean> callback;

    TokenConsumer(Consumer<Boolean> successCallback) {
        callback = successCallback;
    }

    @Override
    protected void handleData(String token) {
        if (token == null) {
            callback.accept(false);
        } else {
            VaadinPlugin.getInstance().getPreferenceStore().setValue(
                    PreferenceConstants.NOTIFICATIONS_USER_TOKEN, token);
            callback.accept(true);
        }
    }

}