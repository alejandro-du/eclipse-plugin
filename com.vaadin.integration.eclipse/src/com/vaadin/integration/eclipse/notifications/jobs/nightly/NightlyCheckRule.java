package com.vaadin.integration.eclipse.notifications.jobs.nightly;

import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * Scheduling rule for checking for new Vaadin versions.
 */
final class NightlyCheckRule implements ISchedulingRule {

    private static final ISchedulingRule INSTANCE = new NightlyCheckRule();

    private NightlyCheckRule() {
    }

    public static ISchedulingRule getInstance() {
        return INSTANCE;
    }

    public boolean contains(ISchedulingRule rule) {
        return this == rule;
    }

    public boolean isConflicting(ISchedulingRule rule) {
        // conflict with new version check
        return this == rule;
    }
}