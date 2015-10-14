package com.vaadin.integration.eclipse.notifications.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

abstract class AbstractNotificationHandleJob extends Job {

    private final String token;
    private final String id;

    protected AbstractNotificationHandleJob(String name, String token,
            String notificationId) {
        super(name);
        setUser(false);
        setSystem(true);

        this.token = token;
        id = notificationId;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        monitor.beginTask(getTaskName(), 1);

        try {
            handleNotification(token, id);
        } finally {
            monitor.worked(1);
            monitor.done();
        }

        return Status.OK_STATUS;
    }

    protected abstract void handleNotification(String token, String id);

    protected abstract String getTaskName();
}
