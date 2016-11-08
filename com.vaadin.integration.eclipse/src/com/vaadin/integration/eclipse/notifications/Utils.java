package com.vaadin.integration.eclipse.notifications;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.notifications.jobs.nightly.ReportVaadinUsageStatistics;
import com.vaadin.integration.eclipse.util.data.DownloadableVaadinVersion;

public final class Utils {

    public static final String ANONYMOUS_ID = ReportVaadinUsageStatistics.ANONYMOUS_ID;

    public static final String SIGN_IN_ICON = "icons.sign-in-icon40";
    public static final String NEW_VERSIONS_ICON = "icons.vaadin-v";
    public static final String NEW_VERSIONS_IMAGE = "icons.vaadin-new-version";

    public static final int MAX_WIDTH = 350;

    public static final String FIRST_POSITION = "{0}";

    public static final String FORWARD_SUFFIX = " \u00BB";

    static final String TOKEN_PLACEHOLDER = "TOKEN";

    static final int FIELD_HEIGHT = 40;

    static final String HELVETICA = "Helvetica";
    static final String ARIAL = "Arial";

    static final String POPUP_LOGO_ICON = "icons.vaadin-icon-16";

    static final String REGULAR_NOTIFICATION_ICON = "icons.bell_grey16";
    static final String NEW_NOTIFICATION_ICON = "icons.bell_red16";
    static final String GO_ICON = "icons.triangle-icon";
    static final String RETURN_ICON = "icons.chevron-left-icon";
    static final String CLEAR_ALL_ICON = "icons.bell-slash-icon";
    static final String NEW_ICON = "icons.dot";

    static final String SIGN_IN_BUTTON = "icons.sign-in-btn";
    static final String SIGN_IN_PRESSED_BUTTON = "icons.sign-in-hit-btn";
    static final String SIGN_IN_HOVER_BUTTON = "icons.sign-in-hover-btn";

    static final String SUBMIT_BUTTON = "icons.submit-btn";
    static final String SUBMIT_PRESSED_BUTTON = "icons.submit-hit-btn";
    static final String SUBMIT_HOVER_BUTTON = "icons.submit-hover-btn";

    static final String NEW_NOTIFICATIONS_ICON = "icons.red-bell";

    private static final Logger LOG = Logger.getLogger(Utils.class.getName());
    static final String SIGN_IN_URL = "https://vaadin.com/notifications-token";
    static final int MIN_HEIGHT = 100;
    static final int PADDING_EDGE = 5;

    static final int ITEM_HEIGHT = 68;

    private Utils() {
    }

    public static FontData[] getModifiedFontData(FontData[] baseData,
            int height) {
        FontData[] styleData = new FontData[baseData.length];
        for (int i = 0; i < styleData.length; i++) {
            FontData base = baseData[i];
            styleData[i] = new FontData(base.getName(), height,
                    base.getStyle());
        }
        return styleData;
    }

    public static FontData[] getModifiedFontData(FontData[] baseData,
            int height, int style) {
        FontData[] styleData = new FontData[baseData.length];
        for (int i = 0; i < styleData.length; i++) {
            FontData base = baseData[i];
            styleData[i] = new FontData(base.getName(), height,
                    base.getStyle() | style);
        }
        return styleData;
    }

    public static Font createFont(int height, int style,
            String... symbolicFontNames) {
        FontRegistry registry = JFaceResources.getFontRegistry();
        FontData[] data = null;
        for (String name : symbolicFontNames) {
            if (registry.hasValueFor(name)) {
                data = registry.getFontData(name);
                break;
            }
        }
        if (data == null) {
            data = registry.defaultFont().getFontData();
        }
        return new Font(PlatformUI.getWorkbench().getDisplay(),
                getModifiedFontData(data, height, style));
    }

    /**
     * Opens URL in EXTERNAL browser. (There was a decision to use external
     * browsers instead of embedded)
     */
    public static IWebBrowser openUrl(String url) throws UrlOpenException {
        boolean urlOpened = true;
        IWebBrowser browser = null;
        try {
            browser = PlatformUI.getWorkbench().getBrowserSupport()
                    .getExternalBrowser();
            browser.openURL(new URL(url));
        } catch (PartInitException exception) {
            LOG.log(Level.SEVERE, null, exception);
            if (Program.launch(url)) {
                LOG.info("URL is opened via external program");
            } else {
                urlOpened = false;
            }
        } catch (MalformedURLException exception) {
            urlOpened = false;
            LOG.log(Level.SEVERE, null, exception);
        }
        if (urlOpened) {
            return browser;
        } else {
            throw new UrlOpenException();
        }
    }

    public static void reportError(String title, String message,
            String reason) {
        ErrorDialog.openError(
                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
                title, message,
                new Status(Status.ERROR, VaadinPlugin.PLUGIN_ID, reason));
    }

    public static boolean isControlClicked(Event event, Control control) {
        if (!control.isDisposed() && !event.widget.isDisposed()
                && control.getShell()
                        .equals(event.widget.getDisplay().getActiveShell())) {

            if (event.widget instanceof Control) {
                return event.widget == control
                        || (control instanceof Composite) && hasParent(
                                (Control) event.widget, (Composite) control);
            }

            Point location = control.getDisplay().getCursorLocation();
            Point listLocation = control.toDisplay(0, 0);
            Point size = control.getSize();
            Rectangle bounds = new Rectangle(listLocation.x, listLocation.y,
                    size.x, size.y);
            return bounds.contains(location);
        } else {
            return false;
        }
    }

    private static String parseBranch(String versionNumber) {
        return versionNumber.substring(0,
                versionNumber.indexOf(".", versionNumber.indexOf(".") + 1));
    }

    private static boolean hasParent(Control subject, Composite composite) {
        if (subject == null) {
            return false;
        } else if (subject.getParent() == composite) {
            return true;
        } else {
            return hasParent(subject.getParent(), composite);
        }
    }

    static class UrlOpenException extends Exception {

        private static final long serialVersionUID = 3105958916452406886L;

    }

}
