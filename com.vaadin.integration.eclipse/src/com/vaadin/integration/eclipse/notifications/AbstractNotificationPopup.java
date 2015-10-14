package com.vaadin.integration.eclipse.notifications;

import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

import com.vaadin.integration.eclipse.notifications.model.Notification;

abstract class AbstractNotificationPopup extends AbstractPopup {

    private ShellActivationListener blockPopupListener;

    private final PopupManager manager = new PopupManagerImpl();

    protected AbstractNotificationPopup() {
        super(ContributionService.getInstance().getContributionControl()
                .getDisplay());
    }

    protected AbstractNotificationPopup(int style) {
        super(ContributionService.getInstance().getContributionControl()
                .getDisplay(), style);
    }

    @Override
    public void closeFade() {
        // the close job can not be extended to handle the blocked popup.
        // therefore the closing fade is checked and the job is rescheduled.
        if (blockPopupListener.isPopupBlocked()) {
            scheduleAutoClose();
            return;
        }
        if (blockPopupListener.isPopupReactivated()) {
            // the popup has been reactivated but a close job will still be
            // active and may be triggered short afterwards. For this reason
            // schedule another time interval
            blockPopupListener.reset();
            scheduleAutoClose();
            return;
        }
        super.closeFade();
    }

    @Override
    public boolean close() {
        if (blockPopupListener != null) {
            PlatformUI.getWorkbench().getDisplay().removeFilter(SWT.Activate,
                    blockPopupListener);
        }
        return super.close();
    }

    @Override
    public void create() {
        super.create();
        registerModalShellListener();
        Label titleLabel = getTitleLabel(getContents());
        if (titleLabel != null) {
            titleLabel.setCursor(getParentShell().getDisplay()
                    .getSystemCursor(SWT.CURSOR_ARROW));
        }
    }

    @Override
    protected void createTitleArea(Composite parent) {
        super.createTitleArea(parent);

        ((GridData) parent.getLayoutData()).heightHint = TITLE_HEIGHT;

        adjustHeader(parent);

        Control[] children = parent.getChildren();
        Control image = children[0];
        Control text = children[1];

        GridData data = (GridData) image.getLayoutData();
        if (data == null) {
            data = new GridData(SWT.LEFT, SWT.CENTER, false, true);
            image.setLayoutData(data);
        }
        data.verticalIndent = 3;

        text.setFont(getBoldFont());
        text.setForeground(getTitleForeground());
    }

    @Override
    protected Color getTitleForeground() {
        return getTextColor();
    }

    @Override
    protected void initializeBounds() {
        Rectangle screenArea = getPrimaryClientArea();
        Shell shell = getShell();
        // superclass computes size with SWT.DEFAULT,SWT.DEFAULT. For long text
        // this causes a large width
        // and a small height. Afterwards the height gets maxed to the
        // MIN_HEIGHT value and the width gets trimmed
        // which results in text floating out of the window
        Point initialSize = shell.computeSize(Utils.MAX_WIDTH, SWT.DEFAULT);
        int height = Math.max(initialSize.y, Utils.MIN_HEIGHT);
        int width = Math.min(initialSize.x, Utils.MAX_WIDTH);

        Point size = new Point(width, height);
        shell.setLocation(
                screenArea.width + screenArea.x - size.x - Utils.PADDING_EDGE,
                screenArea.height + screenArea.y - size.y - Utils.PADDING_EDGE);
        shell.setSize(size);
    }

    protected PopupManager getManager() {
        return manager;
    }

    private Rectangle getPrimaryClientArea() {
        Monitor primaryMonitor = getShell().getDisplay().getPrimaryMonitor();
        return primaryMonitor != null ? primaryMonitor.getClientArea()
                : getShell().getDisplay().getClientArea();
    }

    private void registerModalShellListener() {
        blockPopupListener = new ShellActivationListener();
        PlatformUI.getWorkbench().getDisplay().addFilter(SWT.Activate,
                blockPopupListener);
    }

    private Label getTitleLabel(Control widget) {
        if (widget instanceof Label) {
            Label label = (Label) widget;
            if (getPopupShellTitle().equals(label.getText())) {
                return label;
            }
        }
        if (widget instanceof Composite) {
            for (Control control : ((Composite) widget).getChildren()) {
                Label label = getTitleLabel(control);
                if (label != null) {
                    return label;
                }
            }
        }
        return null;
    }

    private final class PopupManagerImpl implements PopupManager {

        public void openNotification(Notification notification) {
            close();

            NotificationsListPopup popup = new NotificationsListPopup(false);
            popup.open(notification);
        }

        public void showNotificationsList() {
            close();

            NotificationsListPopup popup = new NotificationsListPopup();
            popup.open();
        }

    }

    private final class ShellActivationListener implements Listener {

        private boolean isPopupBlocked;
        private boolean isPopupReactivated;
        private String title;
        private final Label titleLabel;

        ShellActivationListener() {
            titleLabel = getTitleLabel(getContents());
            title = titleLabel == null ? getPopupShellTitle()
                    : titleLabel.getText();
            updateStatus();
        }

        public void handleEvent(Event event) {
            if (!isPopupOpen()) {
                return;
            }
            Widget window = event.widget;
            if (window instanceof Shell) {
                Shell shell = (Shell) window;
                if (isVisibleAndModal(shell)) {
                    if (!isPopupBlocked()) {
                        deactivate();
                    }
                } else if (isPopupBlocked()) {
                    isPopupReactivated = true;
                    activate();
                }
            }
        }

        private void updateStatus() {
            // check existing shells
            IWorkbench workbench = PlatformUI.getWorkbench();
            for (Shell shell : workbench.getDisplay().getShells()) {
                if (isVisibleAndModal(shell) && !isPopupBlocked()) {
                    deactivate();
                    break;
                }
            }
        }

        private boolean isPopupBlocked() {
            return isPopupBlocked;
        }

        private boolean isPopupReactivated() {
            return isPopupReactivated;
        }

        private void reset() {
            isPopupReactivated = false;
        }

        private boolean isPopupOpen() {
            return getShell() != null && !getShell().isDisposed();
        }

        private boolean isVisibleAndModal(Shell shell) {
            int modal = SWT.APPLICATION_MODAL | SWT.SYSTEM_MODAL
                    | SWT.PRIMARY_MODAL;
            return shell.isVisible() && (shell.getStyle() & modal) != 0;
        }

        private void deactivate() {
            isPopupBlocked = true;
            if (titleLabel != null) {
                titleLabel.setText(MessageFormat
                        .format(Messages.Notifications_waitingFocus, title));
            }
        }

        private void activate() {
            isPopupBlocked = false;
            if (titleLabel != null) {
                titleLabel.setText(title);
            }
        }
    }
}
