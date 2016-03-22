package com.vaadin.integration.eclipse.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

import com.vaadin.integration.eclipse.VaadinPlugin;

/**
 * Initializes the preferences to their default values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = VaadinPlugin.getInstance()
                .getPreferenceStore();
        store.setDefault(
                PreferenceConstants.NOTIFICATIONS_NEW_VERSION_POLLING_INTERVAL,
                NotificationsPollingSchedule.PER_FOUR_HOUR.getSeconds());
        store.setDefault(
                PreferenceConstants.NOTIFICATIONS_CENTER_POLLING_INTERVAL,
                NotificationsPollingSchedule.PER_FOUR_HOUR.getSeconds());

        store.setDefault(PreferenceConstants.NOTIFICATIONS_ENABLED, true);

        store.setDefault(PreferenceConstants.NOTIFICATIONS_CENTER_POPUP_ENABLED,
                true);
        store.setDefault(
                PreferenceConstants.NOTIFICATIONS_NEW_VERSION_POPUP_ENABLED,
                true);

        store.setDefault(PreferenceConstants.PRERELEASE_ARCHETYPES_ENABLED,
                false);

        // TODO decide default value
        store.setDefault(
                PreferenceConstants.MAVEN_WIDGETSET_AUTOMATIC_BUILD_ENABLED,
                false);

        /*
         * Migrate old settings here if they exists.
         */
        String oldPreference = store
                .getString("disableAllNotificationsPreference");
        if (oldPreference != null && !oldPreference.isEmpty()) {
            store.setValue(
                    PreferenceConstants.NOTIFICATIONS_NEW_VERSION_POPUP_ENABLED,
                    !store.getBoolean("disableAllNotificationsPreference"));
        }

        store.setDefault(PreferenceConstants.NOTIFICATIONS_USER_TOKEN, "");
        store.setDefault(PreferenceConstants.NOTIFICATIONS_FETCH_ON_START,
                true);
        store.setDefault(PreferenceConstants.NOTIFICATIONS_FETCH_ON_OPEN,
                false);

        store.setDefault(PreferenceConstants.NOTIFICATIONS_VERSION_UPDATE_ITEM,
                true);

        store.addPropertyChangeListener(new VersionsPopupPreferenceUpdater());
    }

    private static final class VersionsPopupPreferenceUpdater
            implements IPropertyChangeListener {

        public void propertyChange(PropertyChangeEvent event) {
            if (event.getProperty().equals(
                    PreferenceConstants.NOTIFICATIONS_CENTER_POPUP_ENABLED)) {
                IPreferenceStore store = VaadinPlugin.getInstance()
                        .getPreferenceStore();
                store.setValue(
                        PreferenceConstants.NOTIFICATIONS_NEW_VERSION_POPUP_ENABLED,
                        store.getBoolean(
                                PreferenceConstants.NOTIFICATIONS_CENTER_POPUP_ENABLED));
            }
        }
    }

}