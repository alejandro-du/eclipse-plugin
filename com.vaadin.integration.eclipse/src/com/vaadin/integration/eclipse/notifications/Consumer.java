package com.vaadin.integration.eclipse.notifications;

public interface Consumer<T> {

    void accept(T t);
}
