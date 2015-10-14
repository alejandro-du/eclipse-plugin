package com.vaadin.integration.eclipse.notifications.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.vaadin.integration.eclipse.notifications.Consumer;
import com.vaadin.integration.eclipse.notifications.model.NotificationsService;

public class GetSettingsJob extends AbstractNotificationJob<String> {

    public GetSettingsJob(Consumer<String> consumer) {
        super("Get notification settings URL", consumer);
        setUser(false);

    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        monitor.beginTask("Requesting user info for settings URL", 1);
        try {
            getConsumer().accept(
                    NotificationsService.getInstance().getSettingsUrl());
            monitor.worked(1);
        } finally {
            monitor.done();
        }

        return Status.OK_STATUS;
    }

}
