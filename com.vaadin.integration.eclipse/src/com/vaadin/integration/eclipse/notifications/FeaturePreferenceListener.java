package com.vaadin.integration.eclipse.notifications;

import java.util.WeakHashMap;
import java.util.logging.Logger;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.notifications.ContributionService.ServiceMediator;
import com.vaadin.integration.eclipse.notifications.jobs.NewNotificationsJob;
import com.vaadin.integration.eclipse.notifications.jobs.nightly.NightlyCheckSchedulerJob;
import com.vaadin.integration.eclipse.preferences.PreferenceConstants;

/**
 * Preference listener that starts or stops polling jobs for notifications and
 * for version updates based on current notification settings.
 */
final class FeaturePreferenceListener
        implements IPropertyChangeListener, BundleListener {

    private static final Logger LOG = Logger
            .getLogger(FeaturePreferenceListener.class.getName());

    private final ServiceMediator mediator;

    private final Display display = PlatformUI.getWorkbench().getDisplay();

    private final WeakHashMap<Job, Void> notificationJobs;
    private final WeakHashMap<Job, Void> versionJobs;

    FeaturePreferenceListener(ServiceMediator mediator) {
        this.mediator = mediator;

        notificationJobs = new WeakHashMap<Job, Void>();
        versionJobs = new WeakHashMap<Job, Void>();
    }

    public void propertyChange(PropertyChangeEvent event) {
        PropertyChangeHandler handler = new PropertyChangeHandler(event);
        if (Display.getCurrent() == null) {
            display.asyncExec(handler);
        } else {
            handler.run();
        }
    }

    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.STOPPED && VaadinPlugin.getInstance()
                .getBundle() == event.getBundle()) {
            event.getBundle().getBundleContext().removeBundleListener(this);
            VaadinPlugin.getInstance().getPreferenceStore()
                    .removePropertyChangeListener(this);
            stopNotificationsJob(
                    "Cancel notifications polling job due stopping bundle");
            stopVersionJobs();
        }
    }

    void versionJobScheduled(NightlyCheckSchedulerJob job) {
        // This method has to be called inside SWT UI thread.
        assert Display.getCurrent() != null;

        versionJobs.put(job, null);
    }

    void notificationsJobScheduled(NewNotificationsJob job) {
        // This method has to be called inside SWT UI thread.
        assert Display.getCurrent() != null;

        notificationJobs.put(job, null);
    }

    private void updateNotificationsPolling() {
        stopNotificationsJob(
                "Cancelling current polling job due change polling interval");
        mediator.schedulePollingJob();
    }

    private void stopNotificationsJob(String logMsg) {
        Job job = mediator.getNotificationsJob().get();
        if (job instanceof NewNotificationsJob) {
            LOG.info(logMsg);
            job.cancel();
        }
        for (Job oldJob : notificationJobs.keySet()) {
            if (oldJob != null) {
                oldJob.cancel();
            }
        }
        notificationJobs.clear();
    }

    private void updateVersionJobs() {
        // Doesn't matter whether jobs have been stopped or not (nothing
        // happens if they are stopped).
        stopVersionJobs();
        if (mediator.isVersionUpdateEnabled()) {
            mediator.startVersionJobs();
        }
    }

    private void enableNotifications(boolean enable) {
        if (enable) {
            mediator.schedulePollingJob();
        } else {
            stopNotificationsJob("Cancelling current polling job due disabling "
                    + "functionality or polling schedule");
        }
    }

    private void stopVersionJobs() {
        Job job = mediator.getVersionsJob().get();
        if (job != null) {
            job.cancel();
        }
        for (Job oldJob : versionJobs.keySet()) {
            if (oldJob != null) {
                oldJob.cancel();
            }
        }
        versionJobs.clear();
    }

    private class PropertyChangeHandler implements Runnable {

        private final PropertyChangeEvent event;

        PropertyChangeHandler(PropertyChangeEvent event) {
            this.event = event;
        }

        public void run() {
            // This method has to be called inside SWT UI thread.
            assert Display.getCurrent() != null;

            if (PreferenceConstants.NOTIFICATIONS_ENABLED
                    .equals(event.getProperty())) {
                enableNotifications(mediator.isNotificationsUpdateEnabled());
                updateVersionJobs();
            } else if (PreferenceConstants.NOTIFICATIONS_CENTER_POLLING_INTERVAL
                    .equals(event.getProperty())) {
                updateNotificationsPolling();
            } else if (PreferenceConstants.NOTIFICATIONS_NEW_VERSION_POLLING_INTERVAL
                    .equals(event.getProperty())) {
                updateVersionJobs();
            }
        }

    }

}