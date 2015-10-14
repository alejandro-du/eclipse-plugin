package com.vaadin.integration.eclipse.notifications.jobs;

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;

import com.vaadin.integration.eclipse.notifications.Consumer;
import com.vaadin.integration.eclipse.notifications.Pair;
import com.vaadin.integration.eclipse.notifications.model.Notification;

/**
 * Trigger scheduled fetching of notifications from the server in the
 * background. For the actual fetching of notifications, see
 * {@link FetchNotificationsJob}.
 *
 * This job is "hidden" : it's system and user doesn't see it in the UI. This is
 * done because it's intended to be scheduled for some delay (otherwise this job
 * will stay in the list of run jobs).
 * 
 * But user should be able to cancel job which does real actions. So new job is
 * created once THIS job executes (see nested job class). This new job is
 * visible for the user and will be shown in the UI (it's created via private
 * CTOR, see below).
 * 
 */
public class NewNotificationsJob
        extends AbstractNotificationJob<NewNotificationsJob> implements
        Consumer<Pair<String, Consumer<Pair<String, Collection<Notification>>>>> {

    public NewNotificationsJob(Consumer<NewNotificationsJob> consumer) {
        super(Messages.Notifications_FetchNewJob, consumer);

        setSystem(true);
        setUser(false);

        setRule(NotificationsUpdateRule.getInstance());
    }

    public void accept(
            Pair<String, Consumer<Pair<String, Collection<Notification>>>> pair) {
        InnerJob job = new InnerJob(pair.getSecond(), pair.getFirst());
        job.schedule();
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        try {
            monitor.beginTask(Messages.Notifications_FetchNewTask, 1);
            if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }
            // this will eventually lead to this.accept() being called to
            // trigger the job (InnerJob) that actually fetches the
            // notifications
            getConsumer().accept(this);
            monitor.worked(1);
        } finally {
            monitor.done();
        }

        return Status.OK_STATUS;
    }

    private static class InnerJob extends FetchNotificationsJob
            implements IJobChangeListener {

        InnerJob(Consumer<Pair<String, Collection<Notification>>> consumer,
                String token) {
            super(Messages.Notifications_FetchNewJob, consumer, token, false);
        }

        public void aboutToRun(IJobChangeEvent event) {
        }

        public void awake(IJobChangeEvent event) {
        }

        public void done(IJobChangeEvent event) {
            if (event.getResult().getSeverity() == IStatus.CANCEL) {
                cancel();
            }
        }

        public void running(IJobChangeEvent event) {
        }

        public void scheduled(IJobChangeEvent event) {
        }

        public void sleeping(IJobChangeEvent event) {
        }

        @Override
        protected String getTaskName() {
            return Messages.Notifications_FetchNewTask;
        }

    }

}
