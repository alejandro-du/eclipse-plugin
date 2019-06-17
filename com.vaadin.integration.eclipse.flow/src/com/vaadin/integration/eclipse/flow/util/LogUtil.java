package com.vaadin.integration.eclipse.flow.util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.vaadin.integration.eclipse.flow.FlowPlugin;

public class LogUtil {

    public static void handleBackgroundException(Throwable t) {
        handleBackgroundException(t.getMessage(), t);
    }

    public static void handleBackgroundException(String message, Throwable t) {
        handleBackgroundException(IStatus.ERROR, message, t);
    }

    public static void handleBackgroundError(String message) {
        handleBackgroundException(message, null);
    }

    public static void handleBackgroundException(int severity, String message,
            Throwable t) {
        IStatus status = t == null
                ? new Status(severity, FlowPlugin.ID, message)
                : new Status(severity, FlowPlugin.ID, message, t);
        FlowPlugin.getInstance().getLog().log(status);
    }

    public static void logInfo(String message) {
        IStatus status = new Status(IStatus.INFO, FlowPlugin.ID, message);
        FlowPlugin.getInstance().getLog().log(status);
    }

    public static void logWarning(String message) {
        IStatus status = new Status(IStatus.WARNING, FlowPlugin.ID, message);
        FlowPlugin.getInstance().getLog().log(status);
    }

    public static void displayError(String message, Throwable ex, Shell shell) {
        MessageDialog.openError(shell, "Error", message);
    }

    public static void displayErrorFromBackgroundThread(final String title,
            final String message) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                Shell shell = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getShell();
                MessageDialog.openError(shell, title, message);
            }
        });
    }

    public static void displayWarning(String message, Shell shell) {
        MessageDialog.openWarning(shell, "Warning", message);
    }

    public static void displayWarningFromBackgroundThread(final String title,
            final String message) {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                Shell shell = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getShell();
                MessageDialog.openWarning(shell, title, message);
            }
        });
    }

    public static CoreException newCoreException(String message, Throwable e) {
        return new CoreException(
                new Status(Status.ERROR, FlowPlugin.ID, message, e));
    }

    public static CoreException newCoreException(String message) {
        return new CoreException(
                new Status(Status.ERROR, FlowPlugin.ID, message));
    }
}
