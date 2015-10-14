package com.vaadin.integration.eclipse.notifications.jobs.nightly;

import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * Scheduling rule for checking for new Vaadin nightly builds.
 */
final class NightlyCheckRule implements ISchedulingRule {

    private static final ISchedulingRule INSTANCE = new NightlyCheckRule();

    private NightlyCheckRule() {
    }

    public static ISchedulingRule getInstance() {
        return INSTANCE;
    }

    public boolean contains(ISchedulingRule rule) {
        // can contain perform upgrade and nightly build check
        return NightlyUpgradeRule.getInstance() == rule || this == rule;
    }

    public boolean isConflicting(ISchedulingRule rule) {
        // conflict with performing upgrade
        // conflict with nightly build check
        return NightlyUpgradeRule.getInstance() == rule || this == rule;
    }
}