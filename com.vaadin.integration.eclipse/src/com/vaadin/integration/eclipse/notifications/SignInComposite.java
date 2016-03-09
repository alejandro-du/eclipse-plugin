package com.vaadin.integration.eclipse.notifications;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.mylyn.commons.ui.compatibility.CommonFonts;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.notifications.Utils.UrlOpenException;

class SignInComposite extends Composite {

    private Font titleFont;
    private Color textColor;
    private Font labelsFont;
    private Font inputFont;

    private Color signInColor;

    private final Listener listener;

    private Text email;
    private Text passwd;

    private Label loginFailedLabel;
    private Label loginInProgress;
    private Color errorColor;

    private PopupUpdateManager manager;

    SignInComposite(Composite parent, PopupUpdateManager updateManager) {
        super(parent, SWT.NO_FOCUS);
        manager = updateManager;

        listener = new Listener();

        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 10;
        setLayout(layout);

        addDisposeListener(listener);
        parent.getDisplay().addFilter(SWT.Traverse, listener);

        initComponenets();
    }

    public void focus() {
        email.forceFocus();
    }

    private void initComponenets() {
        Label title = new Label(this, SWT.NONE);
        titleFont = Utils.createFont(18, SWT.NORMAL, Utils.HELVETICA,
                Utils.ARIAL);
        labelsFont = Utils.createFont(12, SWT.NORMAL, Utils.HELVETICA,
                Utils.ARIAL);
        inputFont = Utils.createFont(14, SWT.NORMAL, Utils.HELVETICA,
                Utils.ARIAL);

        textColor = new Color(getDisplay(), 70, 68, 64);
        signInColor = new Color(getDisplay(), 0, 180, 240);

        title.setFont(titleFont);
        title.setForeground(textColor);

        title.setText(Messages.Notifications_SignIn);
        GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
                .align(SWT.FILL, SWT.TOP).applyTo(title);

        addEmail();
        addPassword();
        addButton();
        addOpenIdAction();
    }

    private void addOpenIdAction() {
        Composite openId = new Composite(this, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
                .indent(SWT.DEFAULT, 10).align(SWT.FILL, SWT.TOP)
                .applyTo(openId);

        GridLayout layout = new GridLayout(2, false);
        openId.setLayout(layout);
        layout.horizontalSpacing = 0;

        NotificationHyperlink link = new NotificationHyperlink(openId);
        link.setText(Messages.Notifications_SignIn);
        link.setFont(CommonFonts.BOLD);
        link.registerMouseTrackListener();
        link.setFont(labelsFont);
        link.setForeground(signInColor);
        GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.TOP).applyTo(link);
        link.addHyperlinkListener(listener);

        // Use hyperlink here to align text with previous hyperlink (label will
        // be shown a bit above the base line of link
        NotificationHyperlink label = new NotificationHyperlink(openId);
        label.setFont(labelsFont);
        label.setForeground(textColor);
        label.setText(Messages.Notifications_SignInWithSuffix);
        GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.TOP).applyTo(label);
        label.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
    }

    private void addButton() {
        final Label button = new Label(this, SWT.NONE);
        Image image = VaadinPlugin.getInstance().getImageRegistry()
                .get(Utils.SIGN_IN_BUTTON);
        button.setBackgroundImage(image);

        button.addMouseListener(listener);
        button.addMouseTrackListener(listener);

        GridDataFactory.fillDefaults().indent(SWT.DEFAULT, 10)
                .hint(image.getImageData().width, image.getImageData().height)
                .align(SWT.LEFT, SWT.TOP).applyTo(button);
        new Label(this, SWT.NONE);
    }

    private void addPassword() {
        Label label = new Label(this, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
                .indent(SWT.DEFAULT, 10).align(SWT.FILL, SWT.TOP)
                .applyTo(label);

        label.setFont(labelsFont);
        label.setText(Messages.Notifications_SignInPassword);
        label.setForeground(textColor);

        Composite wrapper = createWrapper();

        passwd = new Text(wrapper, SWT.PASSWORD);
        passwd.setFont(inputFont);
        passwd.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
        GridDataFactory.fillDefaults().grab(true, true)
                .align(SWT.FILL, SWT.CENTER).applyTo(passwd);

    }

    private Composite createWrapper() {
        Composite wrapper = new Composite(this, SWT.BORDER);
        wrapper.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
        GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
                .hint(SWT.DEFAULT, Utils.FIELD_HEIGHT)
                .align(SWT.FILL, SWT.FILL).applyTo(wrapper);
        wrapper.setLayout(new GridLayout(1, false));
        return wrapper;
    }

    private void addEmail() {
        Label label = new Label(this, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
                .indent(SWT.DEFAULT, 5).align(SWT.FILL, SWT.TOP).applyTo(label);

        label.setFont(labelsFont);
        label.setText(Messages.Notifications_SignInEmail);
        label.setForeground(textColor);

        Composite wrapper = createWrapper();

        email = new Text(wrapper, SWT.NONE);
        email.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
        email.setFont(inputFont);
        GridDataFactory.fillDefaults().grab(true, true)
                .align(SWT.FILL, SWT.CENTER).applyTo(email);
    }

    private void login() {
        String mail = email.getText().trim();
        String pwd = passwd.getText();
        showOperationProgress();
        ContributionService.getInstance().login(mail, pwd, listener);
    }

    private void showOperationProgress() {
        loginInProgress = new Label(this, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
                .indent(3, 0).align(SWT.CENTER, SWT.TOP)
                .applyTo(loginInProgress);
        loginInProgress.setText(Messages.Notifications_SignInProgress);
        loginInProgress.setForeground(signInColor);
        loginInProgress.setFont(labelsFont);
        layout();
    }

    private void notifyFailedLogin() {
        if (loginFailedLabel == null) {
            loginFailedLabel = new Label(this, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
                    .indent(3, 0).align(SWT.FILL, SWT.TOP)
                    .applyTo(loginFailedLabel);
            loginFailedLabel.setText(Messages.Notifications_SignInError);
            errorColor = new Color(getDisplay(), 181, 3, 3);
            loginFailedLabel.setForeground(errorColor);
            loginFailedLabel.setFont(labelsFont);
            layout();
        } else {
            loginFailedLabel.setVisible(true);
        }
    }

    private void showTokenInput() {
        manager.showTokenInput();
    }

    /**
     * This class implements {@link Consumer} interface in order to be a
     * callback for login , see {@link SignInComposite#login()}.
     *
     */
    private class Listener extends HyperlinkAdapter implements DisposeListener,
            MouseListener, Consumer<Boolean>, MouseTrackListener,
            org.eclipse.swt.widgets.Listener {

        public void widgetDisposed(DisposeEvent e) {
            if (titleFont != null) {
                titleFont.dispose();
                titleFont = null;
                labelsFont.dispose();
                labelsFont = null;
                textColor.dispose();
                textColor = null;
                signInColor.dispose();
                signInColor = null;
                inputFont.dispose();
                inputFont = null;
            }
            if (errorColor != null) {
                errorColor.dispose();
                errorColor = null;
            }
            Display.getCurrent().removeFilter(SWT.Traverse, this);
        }

        @Override
        public void linkActivated(HyperlinkEvent e) {
            try {
                showTokenInput();
                Utils.openUrl(Utils.SIGN_IN_URL);
            } catch (UrlOpenException exception) {
                Utils.reportError(Messages.Notifications_BrowserFailTitle,
                        Messages.Notifications_TokenUrlFailMsg,
                        Messages.Notifications_BrowserFailReason);
            }
        }

        public void accept(Boolean success) {
            loginInProgress.dispose();
            loginInProgress = null;
            if (success) {
                manager.showNotificationsList();
                dispose();
            } else {
                passwd.setText("");
                notifyFailedLogin();
            }
        }

        public void mouseDoubleClick(MouseEvent e) {
            login();
            Label button = (Label) e.widget;
            button.setImage(VaadinPlugin.getInstance().getImageRegistry()
                    .get(Utils.SIGN_IN_PRESSED_BUTTON));
        }

        public void mouseDown(MouseEvent e) {
            final Label button = (Label) e.widget;
            button.setBackgroundImage(VaadinPlugin.getInstance()
                    .getImageRegistry().get(Utils.SIGN_IN_PRESSED_BUTTON));
        }

        public void mouseUp(MouseEvent e) {
            Label button = (Label) e.widget;
            button.setBackgroundImage(VaadinPlugin.getInstance()
                    .getImageRegistry().get(Utils.SIGN_IN_BUTTON));
            login();
        }

        public void mouseEnter(MouseEvent e) {
            Label button = (Label) e.widget;
            button.setBackgroundImage(VaadinPlugin.getInstance()
                    .getImageRegistry().get(Utils.SIGN_IN_HOVER_BUTTON));
        }

        public void mouseExit(MouseEvent e) {
            Label button = (Label) e.widget;
            button.setBackgroundImage(VaadinPlugin.getInstance()
                    .getImageRegistry().get(Utils.SIGN_IN_BUTTON));
        }

        public void mouseHover(MouseEvent e) {
        }

        public void handleEvent(Event event) {
            if (SWT.TRAVERSE_RETURN == event.detail) {
                if (event.widget == SignInComposite.this) {
                    login();
                }
            }
        }

    }

}
