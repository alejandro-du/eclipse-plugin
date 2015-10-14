package com.vaadin.integration.eclipse.notifications;

import java.text.MessageFormat;
import java.util.Collection;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.notifications.model.Notification;

class FewNotificationsComposite extends AbstractNotificationComposite {

    private static final String SECOND_PARAM = "{1}";
    private final int notificationsSize;

    private Color red;
    private Color blue;

    private final ItemStyle style = new ItemStyle();

    FewNotificationsComposite(Composite parent, PopupManager manager,
            Collection<Notification> notifications) {
        super(parent, new FewNotifications(), manager, false);
        notificationsSize = notifications.size();

        addDisposeListener(new DisposeHandler());
    }

    @Override
    protected void showDetails() {
        getManager().showNotificationsList();
    }

    @Override
    protected Control createInfoSection() {
        StyledText text = new StyledText(this, SWT.NO_FOCUS);
        text.setEditable(false);

        text.setForeground(getItemTextColor());
        text.setFont(getItemFont());

        // TODO : red color ?
        red = new Color(getDisplay(), 255, 0, 0);
        blue = new Color(getDisplay(), 0, 180, 240);

        String msg = Messages.Notifications_SeveralNotificationsMessage;

        String notificationMsg = Messages.Notifications_SeveralNotificationsMsgParameter;

        text.setText(
                MessageFormat.format(msg, notificationsSize, notificationMsg));

        styleMessage(text, msg, notificationMsg);

        GridDataFactory.fillDefaults().grab(true, false)
                .align(SWT.LEFT, SWT.CENTER).applyTo(text);
        return text;
    }

    @Override
    protected Font getItemFont() {
        return style.getFont();
    }

    @Override
    protected Color getItemTextColor() {
        return style.getTextColor();
    }

    private void applyStyle(StyledText text, Color color, int index,
            int length) {
        StyleRange styleRange = new StyleRange();
        styleRange.start = index;
        styleRange.length = length;
        styleRange.foreground = color;
        text.setStyleRange(styleRange);
    }

    /**
     * Color the number (if any) by {@code red} color and the notifications (if
     * any) by {@code blue} color
     */
    private void styleMessage(StyledText text, String msg,
            String notificationMsg) {
        int numberIndex = msg.indexOf(Utils.FIRST_POSITION);
        int notificationsIndex = msg.indexOf(SECOND_PARAM);

        int numberLength = Integer.toString(notificationsSize).length();

        if (numberIndex != -1 || notificationsIndex != -1) {
            if (numberIndex == -1) {
                applyStyle(text, blue, notificationsIndex,
                        notificationMsg.length());
            } else if (notificationsIndex == -1) {
                applyStyle(text, red, numberIndex, numberLength);
            } else if (numberIndex < notificationsIndex) {
                applyStyle(text, red, numberIndex, numberLength);

                applyStyle(
                        text, blue, notificationsIndex
                                - Utils.FIRST_POSITION.length() + numberLength,
                        notificationMsg.length());
            } else {
                applyStyle(text, blue, notificationsIndex,
                        notificationMsg.length());

                applyStyle(text, red,
                        numberIndex - Utils.FIRST_POSITION.length()
                                + notificationMsg.length(),
                        numberLength);
            }
        }
    }

    private class DisposeHandler implements DisposeListener {

        public void widgetDisposed(DisposeEvent e) {
            if (red != null) {
                red.dispose();
                blue.dispose();
            }
            style.dispose();
        }

    }

    private static class FewNotifications extends Notification {

        FewNotifications() {
            super();
        }

        @Override
        public boolean isRead() {
            return true;
        }

        @Override
        public Image getIcon() {
            return VaadinPlugin.getInstance().getImageRegistry()
                    .get(Utils.NEW_NOTIFICATIONS_ICON);
        }
    }

}
