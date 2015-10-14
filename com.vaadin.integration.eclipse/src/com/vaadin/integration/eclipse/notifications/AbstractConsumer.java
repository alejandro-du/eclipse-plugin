package com.vaadin.integration.eclipse.notifications;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

/**
 * Instances of this class are used as a callbacks/observers in jobs to execute
 * {@link AbstractConsumer#handleData(Object)} method with the result of the
 * task in SWT UI thread.
 */
abstract class AbstractConsumer<T> implements Consumer<T>, Runnable {

    private final AtomicReference<T> ref;
    private final Display display;

    AbstractConsumer() {
        display = PlatformUI.getWorkbench().getDisplay();
        ref = new AtomicReference<T>();
    }

    public void run() {
        handleData(ref.get());
        ref.set(null);
    }

    public void accept(T notifications) {
        ref.set(notifications);
        if (!display.isDisposed()) {
            display.asyncExec(this);
        }
    }

    protected abstract void handleData(T data);

}