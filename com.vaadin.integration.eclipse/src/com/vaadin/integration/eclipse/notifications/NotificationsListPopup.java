package com.vaadin.integration.eclipse.notifications;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.mylyn.commons.workbench.forms.ScalingHyperlink;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.notifications.ContributionService.PopupViewMode;
import com.vaadin.integration.eclipse.notifications.Utils.UrlOpenException;
import com.vaadin.integration.eclipse.notifications.model.Notification;
import com.vaadin.integration.eclipse.notifications.model.VersionUpdateNotification;

/**
 * Shows a list of all notifications and allows to navigate to the specific
 * notification.
 * 
 * @author denis
 *
 */
class NotificationsListPopup extends AbstractPopup {

    private static final int MAX_HEIGHT = 400;
    private static final int RIGHT_SHIFT = 5;

    private static final int ITEMS_LIMIT = 100;

    private final Composite nullComposite = new Composite(new Shell(),
            SWT.NONE);

    private NotificationsListComposite notificationsList;

    private final UpdateManagerImpl updateManager = new UpdateManagerImpl();
    private ActiveControlListener mouseListener = new ActiveControlListener();

    private StackLayout mainLayout;

    private Control signOutWidget;

    private Control settingsLink;

    private Label titleImageLabel;

    private Label titleTextLabel;

    private Control clearAll;

    private BackWidget returnLink;

    private boolean showContent;

    NotificationsListPopup() {
        this(true);
    }

    NotificationsListPopup(boolean showContentInitially) {
        super(ContributionService.getInstance().getContributionControl()
                .getDisplay());
        setDelayClose(-1);
        showContent = showContentInitially;
    }

    @Override
    public void create() {
        switch (ContributionService.getInstance().getViewMode()) {
        case SIGN_IN:
        case TOKEN:
        case NOTIFICATION:
            showContent = false;
            break;
        case LIST:
            break;
        }

        super.create();

        mouseListener = new ActiveControlListener();
        PlatformUI.getWorkbench().getDisplay().addFilter(SWT.MouseDown,
                mouseListener);
        PlatformUI.getWorkbench().getDisplay().addFilter(SWT.FocusOut,
                mouseListener);
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()
                .addListener(SWT.Resize, mouseListener);

        switch (ContributionService.getInstance().getViewMode()) {
        case SIGN_IN:
            updateManager.showSignIn();
            break;
        case TOKEN:
            updateManager.showTokenInput();
            break;
        case NOTIFICATION:
            updateManager.showNotification(ContributionService.getInstance()
                    .getSelectedNotification());
            break;
        case LIST:
            break;
        }
    }

    @Override
    public boolean close() {
        IWorkbenchWindow window = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow();
        if (window == null || window.getShell() == null
                || window.getShell().getDisplay() == null) {
            return true;
        }
        window.getShell().getDisplay().removeFilter(SWT.MouseDown,
                mouseListener);
        window.getShell().removeListener(SWT.Resize, mouseListener);
        PlatformUI.getWorkbench().getDisplay().removeFilter(SWT.FocusOut,
                mouseListener);
        nullComposite.getShell().dispose();
        nullComposite.dispose();
        return super.close();
    }

    @Override
    protected void createTitleArea(Composite parent) {
        ((GridData) parent.getLayoutData()).heightHint = TITLE_HEIGHT;

        adjustHeader(parent);

        titleImageLabel = new Label(parent, SWT.NONE);
        titleImageLabel.setImage(getPopupShellImage(TITLE_HEIGHT));
        titleImageLabel.setVisible(showContent);

        GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).indent(0, 3)
                .applyTo(titleImageLabel);

        titleTextLabel = new Label(parent, SWT.NONE);
        titleTextLabel.setText(getPopupShellTitle());
        titleTextLabel.setFont(getBoldFont());
        titleTextLabel.setForeground(getTitleForeground());
        titleTextLabel
                .setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
        titleTextLabel.setVisible(showContent);

        clearAll = createClearAll(parent);
        clearAll.setVisible(showContent);
        clearAll.setFont(getRegularFont());
        clearAll.setForeground(getTextColor());

        GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).indent(0, 2)
                .applyTo(clearAll);
    }

    @Override
    protected Color getTitleForeground() {
        return getTextColor();
    }

    @Override
    protected void createContentArea(Composite parent) {
        super.createContentArea(parent);

        // composite below title
        Composite pane = new Composite(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout(1, false);
        cancelVerticalSpace(gridLayout);

        GridDataFactory.fillDefaults().grab(true, true)
                .align(SWT.FILL, SWT.FILL).applyTo(pane);
        pane.setLayout(gridLayout);
        gridLayout.marginWidth = 0;

        // main composite whose content is dynamic
        Composite main = new Composite(pane, SWT.NO_FOCUS);
        GridDataFactory.fillDefaults().grab(true, true)
                .align(SWT.FILL, SWT.FILL).applyTo(main);
        mainLayout = new StackLayout();
        main.setLayout(mainLayout);

        notificationsList = createListArea(main);
        if (showContent) {
            mainLayout.topControl = notificationsList;
        } else {
            mainLayout.topControl = new Label(main, SWT.NONE);
        }

        // toolbar bottom composite below content
        createToolBar(pane);
    }

    @Override
    protected void initializeBounds() {
        Shell shell = getShell();
        Point initialSize = shell.computeSize(Utils.MAX_WIDTH, MAX_HEIGHT);
        int height = Math.min(initialSize.y, MAX_HEIGHT);
        int width = Math.min(initialSize.x, Utils.MAX_WIDTH);

        Point location = ContributionService.getInstance()
                .getContributionControl().toDisplay(new Point(0, 0));
        location.x = location.x - Utils.PADDING_EDGE;
        location.y = location.y - height - Utils.PADDING_EDGE;

        Point mainWindowSize = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getShell().getSize();
        Point mainWindowLocation = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getShell().getLocation();
        if (location.x + width > mainWindowLocation.x + mainWindowSize.x) {
            location.x = mainWindowLocation.x + mainWindowSize.x - width;
        }
        location.x -= RIGHT_SHIFT;

        Point size = new Point(width, height);
        shell.setLocation(location);
        shell.setSize(size);
    }

    void open(Notification notification) {
        open();
        updateManager.showNotification(notification);
    }

    void open(boolean refreshOnOpen) {
        open();
        if (refreshOnOpen) {
            ContributionService.getInstance().refreshNotifications(
                    new RefreshCallback(mainLayout.topControl));
        }
    }

    private Control createClearAll(Composite parent) {
        ScalingHyperlink link = new NotificationHyperlink(parent);
        link.setText(Messages.Notifications_ClearAll);
        link.registerMouseTrackListener();
        link.setImage(VaadinPlugin.getInstance().getImageRegistry()
                .get(Utils.CLEAR_ALL_ICON));
        link.addHyperlinkListener(updateManager);

        return link;
    }

    private void createToolBar(Composite parent) {
        Composite toolBar = new Composite(parent, SWT.NO_FOCUS);
        GridData data = new GridData(SWT.FILL, SWT.CENTER, true, false);
        data.heightHint = TITLE_HEIGHT;
        toolBar.setLayoutData(data);

        GridLayout layout = new GridLayout(2, false);
        toolBar.setLayout(layout);
        cancelVerticalSpace(layout);

        signOutWidget = createAction(toolBar, SWT.LEFT,
                Messages.Notifications_SignOut);
        settingsLink = createAction(toolBar, SWT.RIGHT,
                Messages.Notifications_Settings);
        updateBottomBar();
    }

    private NotificationsListComposite createListArea(Composite pane) {
        return createListArea(pane, ITEMS_LIMIT);
    }

    private NotificationsListComposite createListArea(Composite pane,
            int limit) {
        NotificationsListComposite composite = new NotificationsListComposite(
                pane, updateManager, limit);
        return composite;
    }

    private Control createAction(Composite parent, int alignment, String text) {
        ScalingHyperlink link = new NotificationHyperlink(parent);
        link.setText(text);
        link.registerMouseTrackListener();
        link.setLayoutData(new GridData(alignment, SWT.CENTER, true, true));
        link.addHyperlinkListener(updateManager);
        link.setForeground(getTextColor());
        link.setFont(getBoldFont());
        return link;
    }

    private void resetNotificationsList(Control activeListControl) {
        Composite main = mainLayout.topControl.getParent();
        NotificationsListComposite newList = createListArea(main);
        if (activeListControl.isVisible()) {
            mainLayout.topControl = newList;
            main.layout();
        }
        notificationsList.dispose();
        notificationsList = newList;

        updateBottomBar();
    }

    private void updateBottomBar() {
        signOutWidget
                .setVisible(ContributionService.getInstance().isSignedIn());
        settingsLink.setVisible(ContributionService.getInstance().isSignedIn()
                && ContributionService.getInstance().getSettingsUrl() != null);
    }

    private class UpdateManagerImpl extends HyperlinkAdapter
            implements PopupUpdateManager {

        public void showSignIn() {
            updateTitle();

            Composite main = mainLayout.topControl.getParent();
            mainLayout.topControl = new SignInComposite(main, updateManager);
            main.layout();

            clearAll.setVisible(false);
            ContributionService.getInstance()
                    .setViewMode(PopupViewMode.SIGN_IN);
        }

        public void showNotification(Notification notification) {
            updateTitle();

            Composite main = mainLayout.topControl.getParent();
            mainLayout.topControl = createInfoWidget(main, notification);
            main.layout();

            clearAll.setVisible(false);
            ContributionService.getInstance()
                    .setViewMode(PopupViewMode.NOTIFICATION);
        }

        public void close() {
            NotificationsListPopup.this.close();
        }

        public void showTokenInput() {
            updateTitle();
            Composite main = mainLayout.topControl.getParent();
            mainLayout.topControl = new TokenInputComposite(main,
                    updateManager);
            main.layout();
            ContributionService.getInstance().setViewMode(PopupViewMode.TOKEN);
        }

        public void showNotificationsList() {
            activateNotificationsList();
            showList();

            ContributionService.getInstance()
                    .signIn(new RefreshCallback(mainLayout.topControl));
        }

        @Override
        public void linkActivated(HyperlinkEvent e) {
            if (returnLink != null && returnLink.isActionSource(e)) {
                handleReturnLink();
            } else if (e.widget == signOutWidget) {
                handleSignOut();
            } else if (e.widget == clearAll) {
                clearAll();
            } else {
                handleSettings();
            }
        }

        public void dismissVersionNotification() {
            ContributionService.getInstance().hideVersionNotification();

            notificationsList = createListArea(notificationsList.getParent());
            // Just navigate back
            handleReturnLink();
        }

        public void revealAllNotifications() {
            Composite main = mainLayout.topControl.getParent();

            notificationsList = createListArea(main, -1);
            mainLayout.topControl = notificationsList;
            main.layout();

        }

        private Composite createInfoWidget(Composite parent,
                Notification notification) {
            if (notification instanceof VersionUpdateNotification) {
                return new VersionsUpgradeInfoComposite(parent,
                        (VersionUpdateNotification) notification, this);
            } else {
                return new NotificationInfoComposite(parent, notification,
                        this);
            }
        }

        private void updateTitle() {
            BackWidget link = setBackLink();

            titleImageLabel.setParent(nullComposite);
            titleTextLabel.setVisible(false);
            link.getParent().layout(true);
        }

        private void clearAll() {
            if (mainLayout.topControl == notificationsList) {
                notificationsList.clearAll();
            }
        }

        private void handleSettings() {
            try {
                Utils.openUrl(
                        ContributionService.getInstance().getSettingsUrl());
            } catch (UrlOpenException exception) {
                Utils.reportError(Messages.Notifications_BrowserFailTitle,
                        Messages.Notifications_SettingsFailMsg,
                        Messages.Notifications_BrowserFailReason);
            }
        }

        private void handleSignOut() {
            showList();

            ContributionService.getInstance()
                    .signOut(new RefreshCallback(mainLayout.topControl));
            signOutWidget.setVisible(false);
        }

        private void handleReturnLink() {
            activateNotificationsList();

            showList();
        }

        private void showList() {
            ContributionService.getInstance().setViewMode(PopupViewMode.LIST);

            Composite main = mainLayout.topControl.getParent();
            mainLayout.topControl = notificationsList;
            main.layout();
        }

        private void activateNotificationsList() {
            returnLink.dispose();
            returnLink = null;

            titleImageLabel.setParent(titleTextLabel.getParent());
            titleTextLabel.setVisible(true);
            titleImageLabel.moveAbove(titleTextLabel);
            titleImageLabel.setVisible(true);
            titleImageLabel.getParent().layout(true);

            clearAll.setVisible(true);
        }

        private BackWidget setBackLink() {
            if (returnLink == null || returnLink.isDisposed()) {
                returnLink = new BackWidget(titleTextLabel.getParent(), this);
                returnLink.moveAbove(titleTextLabel);
            }
            return returnLink;
        }

    }

    private class ActiveControlListener implements Listener, Runnable {

        public void handleEvent(Event event) {
            if (event.widget.isDisposed() || getShell() == null) {
                return;
            }
            if (isCloseEvent(event)) {
                getShell().getDisplay().removeFilter(SWT.MouseDown,
                        mouseListener);
                // There can be "deadlock like" situation: if close() is
                // scheduled via async call while main window is resizing then
                // UI freezes.
                // It's not really caused by deadlock in Java code: no two
                // threads awaiting each other but SWT UI thread freezes on some
                // OS related call (send event) (at least on Mac). It looks like
                // this is a bug in SWT.
                if (Utils.isControlClicked(event, ContributionService
                        .getInstance().getContributionControl())) {
                    // Move closing operation out of filter events handling (do
                    // it after this handling).
                    event.widget.getDisplay().asyncExec(this);
                } else {
                    close();
                }
            }
        }

        public void run() {
            close();
        }

        private boolean isCloseEvent(Event event) {
            // Be very careful with this logic : there can be unexpected effects
            // (leading to Exceptions) if this is written inaccurate (f.e.
            // layout may cause sending focus event)
            Point location = event.widget.getDisplay().getCursorLocation();
            return !shellContainsWidget(event.widget)
                    && !getShell()
                            .equals(event.widget.getDisplay().getActiveShell())
                    && !getShell().isDisposed()
                    && !getShell().getBounds().contains(location);
        }

        private boolean shellContainsWidget(Widget widget) {
            if (widget instanceof Control) {
                return ((Control) widget).getShell() == getShell();
            }
            return false;
        }
    }

    private class RefreshCallback implements Runnable {

        private final Control activeListControl;

        RefreshCallback(Control control) {
            activeListControl = control;
        }

        public void run() {
            if (mainLayout.topControl == activeListControl
                    && !activeListControl.isDisposed()) {
                resetNotificationsList(activeListControl);
            }
        }

    }

    class BackWidget extends Composite {

        private final IHyperlinkListener listener;

        BackWidget(Composite parent, IHyperlinkListener listener) {
            super(parent, SWT.NO_FOCUS);

            this.listener = listener;
            initComponents();
        }

        boolean isActionSource(HyperlinkEvent event) {
            return event.widget == getChildren()[1];
        }

        private void initComponents() {
            GridLayout layout = new GridLayout(2, false);
            setLayout(layout);

            cancelVerticalSpace(layout);

            Label label = new Label(this, SWT.NONE);
            label.setImage(VaadinPlugin.getInstance().getImageRegistry()
                    .get(Utils.RETURN_ICON));

            NotificationHyperlink link = new NotificationHyperlink(this);
            link.setText(Messages.Notifications_BackAction);
            link.registerMouseTrackListener();
            link.addHyperlinkListener(listener);

            link.setFont(getRegularFont());
            link.setForeground(getTextColor());
        }

    }

}
