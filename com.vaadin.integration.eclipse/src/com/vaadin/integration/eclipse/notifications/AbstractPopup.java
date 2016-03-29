package com.vaadin.integration.eclipse.notifications;

import org.eclipse.jface.window.Window;
import org.eclipse.mylyn.commons.ui.dialogs.AbstractNotificationPopup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.vaadin.integration.eclipse.VaadinPlugin;

abstract class AbstractPopup extends AbstractNotificationPopup {

    private Font boldPopupFont;
    private Font regularFont;

    private Color textColor;
    private Color bckgrnd;

    static final int TITLE_HEIGHT = 36;

    // copied from superclass because private
    private Shell shell;

    protected AbstractPopup(Display display) {
        super(display);

        textColor = new Color(display, 160, 159, 145);
        bckgrnd = new Color(display, 226, 226, 216);
    }

    protected AbstractPopup(Display display, int style) {
        super(display, style);

        textColor = new Color(display, 160, 159, 145);
        bckgrnd = new Color(display, 226, 226, 216);
    }

    @Override
    protected void configureShell(Shell newShell) {
        shell = newShell;
        super.configureShell(newShell);
    }

    // a workaround for fixupDisplayBounds problem in superclass
    @Override
    public int open() {
        if (shell == null || shell.isDisposed()) {
            shell = null;
            create();
        }

        constrainShellSize();
        // location will be constrained by subclasses if necessary
        // shell.setLocation(fixupDisplayBounds(shell.getSize(),
        // shell.getLocation()));

        shell.setVisible(true);

        return Window.OK;
    }

    @Override
    public void setFadingEnabled(boolean fadingEnabled) {
        if (fadingEnabled) {
            // would require copying internals of Mylyn
            // AbstractNotificationPopup.open() etc.
            throw new IllegalArgumentException(
                    "AbstractPopup does not support fading");
        }
    }

    @Override
    public void create() {
        boldPopupFont = Utils.createFont(13, SWT.BOLD, Utils.HELVETICA,
                Utils.ARIAL);
        regularFont = Utils.createFont(13, SWT.NORMAL, Utils.HELVETICA,
                Utils.ARIAL);
        super.create();
        getShell().addDisposeListener(new CleanupListener());
    }

    @Override
    protected Control createContents(Composite parent) {
        Control content = super.createContents(parent);
        // reset gradient background image
        Composite panel = (Composite) content;
        panel.setBackgroundMode(SWT.INHERIT_NONE);
        for (Control control : panel.getChildren()) {
            control.setBackground(bckgrnd);
        }
        return content;
    }

    @Override
    protected String getPopupShellTitle() {
        return Messages.Notifications_PopupNotificationsTitle;
    }

    @Override
    protected Image getPopupShellImage(int maximumHeight) {
        return VaadinPlugin.getInstance().getImageRegistry()
                .get(Utils.REGULAR_NOTIFICATION_ICON);
    }

    @Override
    protected void createContentArea(Composite parent) {
        adjustMargins(parent);
    }

    protected void adjustMargins(Composite parent) {
        GridLayout layout = (GridLayout) parent.getLayout();
        layout.marginLeft = 0;
        layout.marginRight = 0;
        layout.marginTop = 0;
        layout.marginBottom = 0;
        layout.verticalSpacing = 0;
        layout.marginHeight = 0;
    }

    protected void adjustHeader(Composite parent) {
        GridLayout layout = (GridLayout) parent.getLayout();

        layout.marginWidth = 3;
        layout.marginRight = 5;
        layout.marginLeft = 5;
        // layout.horizontalSpacing = 3;
        cancelVerticalSpace(layout);
    }

    protected void cancelVerticalSpace(GridLayout layout) {
        layout.verticalSpacing = 0;
        layout.marginTop = 0;
        layout.marginBottom = 0;
        layout.marginHeight = 0;
    }

    protected Color getTextColor() {
        return textColor;
    }

    protected Font getBoldFont() {
        return boldPopupFont;
    }

    protected Font getRegularFont() {
        return regularFont;
    }

    private class CleanupListener implements DisposeListener {
        public void widgetDisposed(DisposeEvent e) {
            if (boldPopupFont != null) {
                boldPopupFont.dispose();
                regularFont.dispose();
                textColor.dispose();
                bckgrnd.dispose();
            }
        }
    }
}
