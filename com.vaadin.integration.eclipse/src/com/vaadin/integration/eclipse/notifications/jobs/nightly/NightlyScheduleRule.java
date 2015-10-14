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
        // can contain perform upgrade and nightly build check as well as
        // upgrade check scheduling
        return NightlyUpgradeRule.getInstance() == rule
                || NightlyCheckRule.getInstance() == rule || this == rule;
    }

    public boolean isConflicting(ISchedulingRule rule) {
        // conflict with performing upgrade
        // conflict with nightly build check
        // conflict with another upgrade check scheduling job
        return NightlyUpgradeRule.getInstance() == rule
                || NightlyCheckRule.getInstance() == rule || this == rule;
    }
}