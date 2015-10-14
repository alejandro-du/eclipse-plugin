package com.vaadin.integration.eclipse.notifications;

import java.text.MessageFormat;
import java.util.Collection;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import com.vaadin.integration.eclipse.notifications.AbstractNotificationItem.ItemStyle;
import com.vaadin.integration.eclipse.notifications.model.Notification;
import com.vaadin.integration.eclipse.notifications.model.SignInNotification;
import com.vaadin.integration.eclipse.notifications.model.VersionUpdateNotification;

class NotificationsListComposite extends ScrolledComposite {

    NotificationsListComposite(Composite parent, PopupUpdateManager manager,
            int limit) {
        super(parent, SWT.NO_FOCUS | SWT.BORDER | SWT.V_SCROLL);

        setExpandHorizontal(true);
        setExpandVertical(true);

        CustomComposite composite = new CustomComposite(this, manager, limit);
        setContent(composite);

        parent.getDisplay().addFilter(SWT.MouseDown, composite);
        addDisposeListener(composite);
        setMinSize(computeSize(0, SWT.DEFAULT));

        doSetBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
    }

    @Override
    public void setBackground(Color color) {
        // Disables ability to set background outside of this class.
    }

    void clearAll() {
        Composite composite = (Composite) getContent();
        for (Control child : composite.getChildren()) {
            if (child instanceof AbstractNotificationItem) {
                ((AbstractNotificationItem) child).setRead();
            }
        }
        ContributionService.getInstance().setReadAll();
    }

    private void doSetBackground(Color color) {
        super.setBackground(color);
    }

    private static final class CustomComposite extends Composite
            implements Listener, DisposeListener {

        private final PopupUpdateManager updateManager;

        private final Color bckgrnd;

        private final int limit;

        private ItemStyle itemStyle;

        CustomComposite(Composite parent, PopupUpdateManager manager,
                int limit) {
            super(parent, SWT.NONE);
            this.updateManager = manager;
            this.limit = limit;

            bckgrnd = new Color(parent.getDisplay(), 225, 225, 225);
            super.setBackground(bckgrnd);

            GridLayout layout = new GridLayout(1, false);
            layout.marginWidth = 0;
            layout.marginHeight = 0;
            layout.verticalSpacing = 2;
            setLayout(layout);

            initComponents();
        }

        @Override
        public void setBackground(Color color) {
            // Disables ability to set background outside of this class.
        }

        public void widgetDisposed(DisposeEvent e) {
            getParent().getDisplay().removeFilter(SWT.MouseDown, this);
            bckgrnd.dispose();
            if (itemStyle != null) {
                itemStyle.dispose();
            }
        }

        public void handleEvent(Event event) {
            if (isVisible() && Utils.isControlClicked(event, this)) {
                for (Control child : getChildren()) {
                    if (child instanceof AbstractNotificationItem) {
                        AbstractNotificationItem item = (AbstractNotificationItem) child;
                        if (Utils.isControlClicked(event, item)) {
                            ((ItemAction) item).runAction(updateManager);
                            break;
                        }
                    }
                }
            }
        }

        private void initComponents() {
            itemStyle = new ItemStyle();
            SignInNotification signIn = ContributionService.getInstance()
                    .getSignInNotification();
            if (signIn != null) {
                setControlLayoutData(new SignInItem(this, signIn, itemStyle));
            }
            VersionUpdateNotification versionNotification = ContributionService
                    .getInstance().getVersionNotification();
            if (versionNotification != null) {
                setControlLayoutData(new VersionUpdateItem(this,
                        versionNotification, itemStyle));
            }
            int i = 0;
            boolean hasMore = false;
            Collection<Notification> notifications = ContributionService
                    .getInstance().getNotifications();
            for (Notification notification : notifications) {
                if (limit == -1 || i < limit) {
                    setControlLayoutData(
                            new NotificationItem(this, notification, itemStyle));
                } else {
                    hasMore = true;
                    break;
                }
                i++;
            }
            if (hasMore) {
                Label label = new Label(this, SWT.NONE);
                GridDataFactory.fillDefaults().grab(true, false)
                        .align(SWT.CENTER, SWT.CENTER).applyTo(label);
                label.setFont(itemStyle.getFont());
                label.setForeground(itemStyle.getTextColor());
                label.setText(MessageFormat.format(
                        Messages.Notifications_MoreNotificationsAvailable,
                        notifications.size() - limit));
                Button button = new Button(this, SWT.PUSH);
                button.setText(Messages.Notifications_ShowMoreNotifications);
                button.setFont(itemStyle.getFont());
                button.setForeground(itemStyle.getTextColor());
                button.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        updateManager.revealAllNotifications();
                    }
                });
                GridDataFactory.fillDefaults().grab(true, false)
                        .align(SWT.CENTER, SWT.CENTER).applyTo(button);
            }
        }

        private void setControlLayoutData(Control item) {
            GridDataFactory.fillDefaults().grab(true, false)
                    .align(SWT.FILL, SWT.FILL).applyTo(item);
            GridData data = (GridData) item.getLayoutData();
            data.heightHint = Utils.ITEM_HEIGHT;
        }
    }

}
