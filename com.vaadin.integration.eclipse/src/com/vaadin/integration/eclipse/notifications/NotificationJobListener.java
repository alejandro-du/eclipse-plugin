package com.vaadin.integration.eclipse.notifications;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.vaadin.integration.eclipse.notifications.ContributionService.ServiceMediator;

/**
 * Listener to re-schedule the notifications polling job when it has completed
 * (unless disabled by preferences).
 */
final class NotificationJobListener extends JobChangeAdapter
        implements Runnable {

    private final ServiceMediator mediator;
    private final Display display;
    private final Runnable callback;
    private final AtomicReference<Job> job;

    NotificationJobListener(ServiceMediator mediator, Runnable runnable) {
        this.mediator = mediator;
        display = PlatformUI.getWorkbench().getDisplay();
        callback = runnable;
        job = new AtomicReference<Job>(null);
    }

    @Override
    public void done(IJobChangeEvent event) {
        if (!display.isDisposed()) {
            job.set(event.getJob());
            if (callback == null) {
                // Polling should be scheduled only on initial notification
                // fetching. The callback availability (!= null) is an
                // indicator that job is initiated by UI action
                display.asyncExec(this);
            } else {
                display.asyncExec(callback);
            }
            event.getJob().removeJobChangeListener(this);
        }
    }

    public void run() {
        Job sourceJob = job.get();
        if (sourceJob != null
                && sourceJob.equals(mediator.getNotificationsJob().get())) {
            // the check above will prevent rescheduling polling if the
            // job has been cancelled because of preferences
            mediator.schedulePollingJob();
        }
    }

}