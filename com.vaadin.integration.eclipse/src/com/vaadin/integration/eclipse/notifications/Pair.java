package com.vaadin.integration.eclipse.notifications;

public class Pair<T, S> {

    private final T first;

    private final S second;

    public Pair(T t, S s) {
        first = t;
        second = s;
    }

    public T getFirst() {
        return first;
    }

    public S getSecond() {
        return second;
    }
}
