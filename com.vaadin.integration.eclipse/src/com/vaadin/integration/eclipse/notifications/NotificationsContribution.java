package com.vaadin.integration.eclipse.notifications;

import java.lang.ref.WeakReference;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.notifications.model.Notification;
import com.vaadin.integration.eclipse.notifications.model.VersionUpdateNotification;
import com.vaadin.integration.eclipse.preferences.PreferenceConstants;

/**
 * This is the main entry point for notifications contribution and all related
 * functionality.
 * 
 * It register the button which shows the notifications list (but also it cares
 * about its parent toolbar in case the button is disabled).
 *
 */
public class NotificationsContribution
        extends WorkbenchWindowControlContribution {

    @Override
    protected Control createControl(final Composite parent) {
        Control control = getControlAccess().doCreateControl(parent);
        if (VaadinPlugin.getInstance().getPreferenceStore()
                .getBoolean(PreferenceConstants.NOTIFICATIONS_ENABLED)) {
            return control;
        } else {
            /*
             * This is hack to hide toolbar depending on settings. Unfortunately
             * it looks like extensions functionality in plugin.xml almost
             * doesn't work for toolbars in status bar.
             */
            getControlAccess().getContributionToolbar(parent).setVisible(false);
            control.getDisplay().asyncExec(new Runnable() {

                public void run() {
                    getControlAccess().hideToolbar();
                    getControlAccess().getContributionToolbar(parent)
                            .setVisible(true);
                }
            });

            return null;
        }
    }

    private static ContributionControlAccess getControlAccess() {
        return ContributionService.getInstance();
    }

    static class ContributionControlAccess {

        private Button control;

        private WeakReference<Composite> parentBar;
        private Composite toolBarParent;

        private boolean isPopupOpen;

        ContributionControlAccess() {
            VaadinPlugin.getInstance().getPreferenceStore()
                    .addPropertyChangeListener(new PreferencesListener());
        }

        Button getContributionControl() {
            return control;
        }

        void updateContributionControl() {
            if (control == null || control.isDisposed()) {
                return;
            }
            for (Notification notification : ContributionService.getInstance()
                    .getNotifications()) {
                if (!notification.isRead()) {
                    control.setImage(getNewIcon());
                    return;
                }
            }
            VersionUpdateNotification versionNotification = ContributionService.getInstance().getVersionNotification();
            if (versionNotification != null && !versionNotification.isRead()) {
                control.setImage(getNewIcon());
            } else {
                control.setImage(getRegularIcon());
            }
        }

        private void setToolBarParent(Composite parent) {
            parentBar = new WeakReference<Composite>(parent.getParent());
            toolBarParent = parent;
        }

        private void showContribution() {
            if (parentBar.get() != null) {
                toolBarParent.setVisible(true);
                toolBarParent.setParent(parentBar.get());
                parentBar.get().layout();
            }
        }

        private void setPopupOpen(boolean visible) {
            isPopupOpen = visible;
        }

        private boolean isPopupOpen() {
            return isPopupOpen;
        }

        private void setControl(Button control) {
            this.control = control;
        }

        private Image getNewIcon() {
            return VaadinPlugin.getInstance().getImageRegistry()
                    .get(Utils.NEW_NOTIFICATION_ICON);
        }

        private Image getRegularIcon() {
            return VaadinPlugin.getInstance().getImageRegistry()
                    .get(Utils.REGULAR_NOTIFICATION_ICON);
        }

        private Control doCreateControl(final Composite parent) {
            Button button = new Button(parent, SWT.PUSH | SWT.FLAT);

            button.getDisplay().addFilter(SWT.MouseDown,
                    new ButtonListener(button));

            init(button);

            if (button.getImage() == null) {
                button.setImage(getControlAccess().getRegularIcon());
            }
            // button.addSelectionListener(new ButtonListener(button));

            return button;
        }

        private void hideToolbar() {
            final ToolBar toolbar = getContributionToolbar(
                    getContributionControl());
            Composite composite = toolbar.getParent();
            Composite parent = composite.getParent();
            getControlAccess().setToolBarParent(composite);
            composite.setVisible(false);
            composite.setParent(PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow().getShell());
            parent.layout();
        }

        private void init(Button control) {
            setControl(control);
            ContributionService.getInstance().initializeContribution();
        }

        private ToolBar getContributionToolbar(Control control) {
            if (control instanceof ToolBar) {
                return (ToolBar) control;
            } else {
                return getContributionToolbar(control.getParent());
            }
        }

    }

    private static class PreferencesListener
            implements IPropertyChangeListener {

        public void propertyChange(PropertyChangeEvent event) {
            if (PreferenceConstants.NOTIFICATIONS_ENABLED
                    .equals(event.getProperty())) {
                if (VaadinPlugin.getInstance().getPreferenceStore().getBoolean(
                        PreferenceConstants.NOTIFICATIONS_ENABLED)) {
                    getControlAccess().showContribution();
                } else {
                    getControlAccess().hideToolbar();
                }
            }
        }

    }

    private static class ButtonListener extends SelectionAdapter
            implements Listener, DisposeListener, Runnable {

        private final Control source;

        ButtonListener(Control control) {
            source = control;
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
            NotificationsListPopup popup = new NotificationsListPopup();
            popup.open();
        }

        public void widgetDisposed(DisposeEvent e) {
            if (e.widget == source) {
                e.widget.getDisplay().removeFilter(SWT.MouseDown, this);
            } else {
                // shell
                getControlAccess().setPopupOpen(false);
            }
        }

        public void run() {
            NotificationsListPopup popup = new NotificationsListPopup();
            popup.open(ContributionService.getInstance().isRefreshOnOpen());
            getControlAccess().setPopupOpen(true);
            popup.getShell().addDisposeListener(this);
        }

        public void handleEvent(Event event) {
            if (event.widget == source && !getControlAccess().isPopupOpen()) {
                // Do this out of filter events handling
                event.widget.getDisplay().asyncExec(this);
            }
        }
    }
}
