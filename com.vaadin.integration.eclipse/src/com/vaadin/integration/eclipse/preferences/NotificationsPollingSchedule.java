package com.vaadin.integration.eclipse.preferences;

public enum NotificationsPollingSchedule {

    PER_HOUR(3600), PER_FOUR_HOUR(4 * 3600), PER_DAY(24 * 3600), NEVER(-1);

    // interval in seconds
    private final int seconds;

    private NotificationsPollingSchedule(int seconds) {
        this.seconds = seconds;
    }

    public int getSeconds() {
        return seconds;
    }
}
