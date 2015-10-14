package com.vaadin.integration.eclipse.notifications.jobs;

import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * Scheduling rule for performing notifications update.
 */
final class NotificationsUpdateRule implements ISchedulingRule {

    private static final ISchedulingRule INSTANCE = new NotificationsUpdateRule();

    private NotificationsUpdateRule() {
    }

    public static ISchedulingRule getInstance() {
        return INSTANCE;
    }

    public boolean contains(ISchedulingRule rule) {
        return this == rule;
    }

    public boolean isConflicting(ISchedulingRule rule) {
        return this == rule;
    }
}