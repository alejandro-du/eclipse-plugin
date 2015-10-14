package com.vaadin.integration.eclipse.notifications;

import java.lang.ref.Reference;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.vaadin.integration.eclipse.notifications.ContributionService.ServiceMediator;

/**
 * Job listener to reschedule the available Vaadin upgrade checking job after it completes.
 */
final class VersionUpdateJobListener extends JobChangeAdapter
        implements Runnable {

    private final Display display;
    private final AtomicReference<Job> job;
    private final Reference<Job> versionJob;
    private final ServiceMediator mediator;

    VersionUpdateJobListener(ServiceMediator mediator,
            Reference<Job> versionJob) {
        display = PlatformUI.getWorkbench().getDisplay();
        job = new AtomicReference<Job>(null);
        this.versionJob = versionJob;
        this.mediator = mediator;
    }

    @Override
    public void done(IJobChangeEvent event) {
        if (!display.isDisposed()) {
            job.set(event.getJob());
            display.asyncExec(this);
            event.getJob().removeJobChangeListener(this);
        }
    }

    public void run() {
        Job sourceJob = job.get();
        if (sourceJob != null && sourceJob.equals(versionJob.get())) {
            // the check above will prevent rescheduling polling if the
            // job has been cancelled because of preferences
            mediator.startVersionJobs();
        }
    }

}