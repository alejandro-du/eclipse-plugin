package com.vaadin.integration.eclipse.notifications.jobs.nightly;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

import com.vaadin.integration.eclipse.notifications.Consumer;
import com.vaadin.integration.eclipse.notifications.ProjectsUpgradeInfo;

/**
 * Background scheduler (not visible in the task list) that triggers nightly
 * build check job. This job is then rescheduled by
 * {@link VersionUpdateJobListener}.
 *
 * This is waiting only job. It's purpose to wait until some delay (via schedule
 * method) and run two real job when it's activated. The reason why it exists :
 * it's invisible in UI. Two inner jobs should be visible to the user to be able
 * to cancel them.
 */
public final class NightlyCheckSchedulerJob extends Job {
    private final Job nightlyCheckJob;
    private final Job checkVaadinVersionsJob;

    private final Consumer<ProjectsUpgradeInfo> consumer;

    public NightlyCheckSchedulerJob(Consumer<ProjectsUpgradeInfo> consumer) {
        super(Messages.Notifications_NightlySchedulerJobName);
        setUser(false);
        setSystem(true);

        this.consumer = consumer;
        nightlyCheckJob = new NightlyJob();
        checkVaadinVersionsJob = new VersionJob();
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        if (monitor.isCanceled()) {
            return Status.CANCEL_STATUS;
        }

        nightlyCheckJob.schedule();
        checkVaadinVersionsJob.schedule();

        return Status.OK_STATUS;
    }

    public void stop() {
        nightlyCheckJob.cancel();
        checkVaadinVersionsJob.cancel();
        cancel();
    }

    /**
     * The reason why these two classes are nested and not static is the
     * articulation that they have strong reference to enclosing ( this
     * NightlyCheckSchedulerJob ) class. The latter class is the only visible
     * class and it should not be removed by GC until its inner jobs are not
     * finished (and can be removed as well).
     * 
     */
    private class NightlyJob extends NightlyCheckJob {

        NightlyJob() {
            NightlyCheckSchedulerJob.this
                    .addJobChangeListener(new JobListener(this));
        }

        @Override
        protected Consumer<ProjectsUpgradeInfo> getConsumer() {
            return consumer;
        }

    }

    private class VersionJob extends ReportVaadinUsageStatistics {

        VersionJob() {
            NightlyCheckSchedulerJob.this
                    .addJobChangeListener(new JobListener(this));
        }
    }

    private static class JobListener extends JobChangeAdapter {

        private final Job job;

        JobListener(Job job) {
            this.job = job;
        }

        @Override
        public void done(IJobChangeEvent event) {
            if (event.getResult().getSeverity() == IStatus.CANCEL) {
                job.cancel();
            }
        }
    }

}