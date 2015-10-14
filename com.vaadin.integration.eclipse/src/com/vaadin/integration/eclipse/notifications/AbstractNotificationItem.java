package com.vaadin.integration.eclipse.notifications;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.notifications.model.Notification;

abstract class AbstractNotificationItem extends Composite {

    protected static final int ITEM_H_MARGIN = 5;

    private static final String DASH = " -";

    private static final DateFormat FORMAT = new SimpleDateFormat(
            "MMMMM d, yyyy");

    private Label newNotificationLabel;

    private final Notification notification;

    private final ItemStyle style;
    // this is currently redundant as no special ItemStyles are used
    private final ItemStyle defaultStyle = new ItemStyle();

    private final boolean hasNewIndicator;

    protected AbstractNotificationItem(Composite parent,
            Notification notification) {
        this(parent, notification, null);
    }

    protected AbstractNotificationItem(Composite parent,
            Notification notification, boolean hasNewIndicator) {
        this(parent, notification, null, hasNewIndicator);
    }

    protected AbstractNotificationItem(Composite parent,
            Notification notification, ItemStyle style) {
        this(parent, notification, style, true);
    }

    protected AbstractNotificationItem(Composite parent,
            Notification notification, ItemStyle style,
            boolean hasNewIndicator) {
        super(parent, SWT.NONE);
        this.notification = notification;
        this.style = style;
        this.hasNewIndicator = hasNewIndicator;

        GridLayout layout = new GridLayout(4, false);
        layout.marginRight = ITEM_H_MARGIN;
        layout.marginLeft = ITEM_H_MARGIN;
        setLayout(layout);

        setCursor(parent.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
        doSetBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));

        addDisposeListener(new DisposeHandler());
    }

    @Override
    public void setBackground(Color color) {
        // Disables ability to set background outside of this class.
    }

    @Override
    public void setLayoutData(Object layoutData) {
        if (newNotificationLabel == null) {
            initComponents();
        }
        super.setLayoutData(layoutData);
    }

    protected final boolean hasNewIndicator() {
        return hasNewIndicator;
    }

    protected Notification getNotification() {
        return notification;
    }

    protected Control createInfoSection() {
        Composite composite = new Composite(this, SWT.NONE);

        GridLayout layout = new GridLayout(2, false);
        layout.horizontalSpacing = 0;
        composite.setLayout(layout);

        Label title = new Label(composite, SWT.NONE);
        title.setText(getSummary());
        title.setFont(getItemFont());
        GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
                .align(SWT.FILL, SWT.CENTER).applyTo(title);
        title.setForeground(getItemTextColor());

        buildShortDescription(style, composite, title);

        return composite;
    }

    protected void buildShortDescription(ItemStyle style, Composite parent,
            Label title) {
        buildShortDecriptionText(parent);

        Label label = new Label(parent, SWT.NONE);
        label.setForeground(getReadMoreColor());
        label.setText(Messages.Notifications_ReadMore);
        label.setFont(getItemFont());
        label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        title.setForeground(getItemTextColor());
    }

    protected String getSummary() {
        return getNotification().getTitle();
    }

    /**
     * Build short description text based on notification.
     * <p>
     * This component will be combined ("concatenated") with "read more >>" text
     * component in {@link #buildShortDescription(ItemStyle, Composite, Label)}
     * from which this method is called.
     */
    protected Control buildShortDecriptionText(Composite parent) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(getDate(getNotification()) + DASH);
        GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER)
                .applyTo(label);
        label.setFont(getItemFont());
        label.setForeground(getItemTextColor());
        return label;
    }

    protected void activate() {
        setRead();
    }

    protected void setRead() {
        getNotification().setRead();
        newNotificationLabel.setImage(null);
    }

    protected Font getItemFont() {
        return style == null ? defaultStyle.getFont() : style.getFont();
    }

    protected Color getItemTextColor() {
        return style == null ? defaultStyle.getTextColor() : style
                .getTextColor();
    }

    private Color getReadMoreColor() {
        return style == null ? defaultStyle.getReadMoreColor() : style
                .getReadMoreColor();
    }

    private String getDate(Notification notification) {
        return FORMAT.format(notification.getDate());
    }

    private void doSetBackground(Color color) {
        super.setBackground(color);
    }

    private void initComponents() {
        newNotificationLabel = new Label(this, SWT.NONE);
        Image newNotificationIcon = VaadinPlugin.getInstance()
                .getImageRegistry().get(Utils.NEW_ICON);
        if (!getNotification().isRead() && hasNewIndicator()) {
            newNotificationLabel.setImage(newNotificationIcon);
        }

        GridDataFactory newNotificationFactory = GridDataFactory.fillDefaults()
                .grab(false, true).align(SWT.CENTER, SWT.CENTER);
        if (getNotification().isRead() && hasNewIndicator()) {
            // this code allows to keep the same indentation regardless of icon
            // presence
            newNotificationFactory
                    .hint(newNotificationIcon.getImageData().width, 0);
        } else if (!hasNewIndicator()) {
            // don't eat any space in case icon label shouldn't be here at all
            newNotificationFactory.hint(0, 0);
        }
        newNotificationFactory.applyTo(newNotificationLabel);

        Label typeLabel = new Label(this, SWT.NONE);
        typeLabel.setImage(notification.getIcon());
        GridDataFactory factory = GridDataFactory.fillDefaults()
                .grab(false, true).align(SWT.CENTER, SWT.CENTER);
        if (hasNewIndicator()) {
            factory.indent(ITEM_H_MARGIN, 0);
        }
        factory.applyTo(typeLabel);

        Control infoSection = createInfoSection();
        if (infoSection.getLayoutData() == null) {
            GridDataFactory.fillDefaults().grab(true, true)
                    .align(SWT.FILL, SWT.CENTER).applyTo(infoSection);
        }

        Label goLabel = new Label(this, SWT.NONE);
        goLabel.setImage(VaadinPlugin.getInstance().getImageRegistry()
                .get(Utils.GO_ICON));
        GridDataFactory.fillDefaults().grab(false, true)
                .align(SWT.RIGHT, SWT.CENTER).applyTo(goLabel);
    }

    private class DisposeHandler implements DisposeListener {

        public void widgetDisposed(DisposeEvent e) {
            style.dispose();
            defaultStyle.dispose();
        }

    }

    /**
     * This class is a holder for some special resources, and creates them on
     * demand. The created resources must be disposed either directly or by
     * calling {@link ItemStyle#dispose()}.
     */
    static class ItemStyle {
        private Font font;
        private Color color;
        private Color readMoreColor;

        ItemStyle() {
        }

        Font getFont() {
            if (font == null) {
                font = Utils.createFont(12, SWT.NORMAL, Utils.HELVETICA, Utils.ARIAL);
            }
            return font;
        }

        Color getTextColor() {
            if (color == null) {
                color = new Color(PlatformUI.getWorkbench().getDisplay(), 154, 150, 143);
            }
            return color;
        }

        Color getReadMoreColor() {
            if (readMoreColor == null) {
                readMoreColor = new Color(PlatformUI.getWorkbench().getDisplay(), 0, 180, 240);
            }
            return readMoreColor;
        }

        void dispose() {
            if (font != null) {
                font.dispose();
            }
            if (color != null) {
                color.dispose();
            }
            if (readMoreColor != null) {
                readMoreColor.dispose();
            }
        }
    }
}
