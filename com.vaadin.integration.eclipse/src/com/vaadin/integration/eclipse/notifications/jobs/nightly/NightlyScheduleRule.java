package com.vaadin.integration.eclipse.notifications.jobs.nightly;

import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * Scheduling rule for scheduling of checks for new Vaadin nightly builds.
 */
final class NightlyScheduleRule implements ISchedulingRule {

    private static final ISchedulingRule INSTANCE = new NightlyScheduleRule();

    private NightlyScheduleRule() {
    }

    public static ISchedulingRule getInstance() {
        return INSTANCE;
    }

    public boolean contains(ISchedulingRule rule) {
        // can contain new version check as well as
        // new version check scheduling
        return NightlyCheckRule.getInstance() == rule || this == rule;
    }

    public boolean isConflicting(ISchedulingRule rule) {
        // conflict with new version check
        // conflict with another new version check scheduling job
        return NightlyCheckRule.getInstance() == rule || this == rule;
    }
}