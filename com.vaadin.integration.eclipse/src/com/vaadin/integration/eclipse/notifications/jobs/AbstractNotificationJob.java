package com.vaadin.integration.eclipse.notifications.jobs;

import org.eclipse.core.runtime.jobs.Job;

import com.vaadin.integration.eclipse.notifications.Consumer;

abstract class AbstractNotificationJob<T> extends Job {

    private final Consumer<T> consumer;

    protected AbstractNotificationJob(String name, Consumer<T> consumer) {
        super(name);
        this.consumer = consumer;

        setUser(false);
        setSystem(true);
    }

    protected Consumer<T> getConsumer() {
        return consumer;
    }

}
