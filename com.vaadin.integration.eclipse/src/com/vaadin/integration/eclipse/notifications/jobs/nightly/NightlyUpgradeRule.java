package com.vaadin.integration.eclipse.notifications.jobs.nightly;

import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * Scheduling rule for performing Vaadin nightly build upgrades.
 */
final class NightlyUpgradeRule implements ISchedulingRule {

    private static final ISchedulingRule INSTANCE = new NightlyUpgradeRule();

    private NightlyUpgradeRule() {
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