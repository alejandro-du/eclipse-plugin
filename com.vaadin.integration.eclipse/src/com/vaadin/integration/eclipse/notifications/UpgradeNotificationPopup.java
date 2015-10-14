package com.vaadin.integration.eclipse.notifications;

import java.text.MessageFormat;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;

import com.vaadin.integration.eclipse.notifications.model.VersionUpdateNotification;

class UpgradeNotificationPopup extends AbstractNotificationPopup {

    private final VersionUpdateNotification notification;

    // Note: currently this notification is not used - see the comments in
    // showDetails() and in the caller of the constructor
    UpgradeNotificationPopup(VersionUpdateNotification notification) {
        this.notification = notification;
    }

    @Override
    protected void createContentArea(Composite parent) {
        super.createContentArea(parent);

        PopupContent control = new PopupContent(parent, notification,
                getManager());
        GridDataFactory.fillDefaults().grab(true, true).span(2, 1)
                .align(SWT.FILL, SWT.FILL).applyTo(control);
        GridData data = (GridData) control.getLayoutData();
        data.heightHint = Utils.ITEM_HEIGHT;
    }

    private static class PopupContent extends AbstractNotificationComposite
            implements DisposeListener {

        private final ItemStyle style = new ItemStyle();

        PopupContent(Composite parent, VersionUpdateNotification notification,
                PopupManager manager) {
            super(parent, notification, manager, false);
            getShell().addDisposeListener(this);
        }

        public void widgetDisposed(DisposeEvent e) {
            style.dispose();
        }

        @Override
        protected void showDetails() {
            activate();
            /*
             * Note: "persisted" version notification which is shown in details
             * is not the same as version notification here. Persisted
             * notification has cumulative info about all updates since last
             * "dismiss". Notification from this popup contains only new info.
             */
            // In current implementation "temporary" notification (provided in
            // CTOR) is not used at all
            ContributionService.getInstance().markRead(
                    ContributionService.getInstance().getVersionNotification());
            getManager().openNotification(
                    ContributionService.getInstance().getVersionNotification());
        }

        @Override
        protected void activate() {
            ContributionService.getInstance().getVersionNotification()
                    .setRead();
        }

        @Override
        protected VersionUpdateNotification getNotification() {
            return (VersionUpdateNotification) super.getNotification();
        }

        @Override
        protected Control createInfoSection() {
            StyledText text = new StyledText(this, SWT.NO_FOCUS | SWT.WRAP) {
                @Override
                public void notifyListeners(int eventType, Event event) {
                    if ((eventType == SWT.MouseDown)) {
                        PopupContent.this.notifyListeners(eventType, event);
                    }
                }
            };
            text.setEditable(false);
            text.setCaret(null);
            text.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
            text.setLineSpacing(5);
            String msg = Messages.Notifications_NewVersionsPopup;
            String more = Messages.Notifications_NewVersionsPopupMore;
            text.setText(MessageFormat.format(msg, more));
            text.setFont(style.getFont());
            text.setForeground(style.getTextColor());

            int index = msg.indexOf(Utils.FIRST_POSITION);
            if (index >= 0) {
                StyleRange styleRange = new StyleRange();
                styleRange.start = index;
                styleRange.length = more.length();
                styleRange.foreground = style.getReadMoreColor();
                text.setStyleRange(styleRange);
            }

            GridDataFactory.fillDefaults().grab(true, true)
                    .align(SWT.FILL, SWT.CENTER).indent(ITEM_H_MARGIN, 0)
                    .applyTo(text);
            return text;
        }
    }

}
