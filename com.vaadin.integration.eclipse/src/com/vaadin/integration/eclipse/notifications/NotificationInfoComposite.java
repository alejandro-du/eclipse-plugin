package com.vaadin.integration.eclipse.notifications;

import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ccil.cowan.tagsoup.Parser;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.vaadin.integration.eclipse.notifications.model.Notification;

/**
 * Composite which shows full info about notification.
 *
 */
class NotificationInfoComposite extends Composite {

    private static final int LEFT_MARGIN = 10;

    private static final String STRIP_HTML_ERROR = "Unable to strip html ";

    private final Notification notification;

    private final PopupUpdateManager manager;

    private Font titleFont;

    private final Listener listener;

    private Font labelsFont;

    private Font footerFont;

    private Color footerColor;

    private static final String HTML_PREFIX = "<style>"
            + "body { margin: 5px; padding:0px;} span {"
            + "font-family:Helvetica, Arial;" + "font-size: 12px;"
            + "font-weight: lighter;" + "} " + "pre {"
            + "font-family: 'Courier New',Courier,monospace;"
            + "font-size:14 px;" + "}" + "</style> <span>";

    private static final String HTML_SUFFIX = "</span>";

    private static final String NEW_LINE = "\n";
    private static final String PRE = "pre";
    private static final String P = "p";
    private static final String BR = "br";

    private static final Logger LOG = Logger
            .getLogger(NotificationInfoComposite.class.getName());

    public NotificationInfoComposite(Composite parent,
            Notification notification, PopupUpdateManager manager) {
        super(parent, SWT.NONE);
        this.notification = notification;
        this.manager = manager;

        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.marginBottom = 10;
        layout.verticalSpacing = 15;
        setLayout(layout);

        listener = new Listener();

        doSetBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));

        addDisposeListener(listener);

        initComponents();
    }

    @Override
    public void setBackground(Color color) {
        // Disables ability to set background outside of this class.
    }

    private void doSetBackground(Color color) {
        super.setBackground(color);
    }

    private void initComponents() {
        Label label = new Label(this, SWT.NONE);
        label.setImage(notification.getHeaderImage());
        GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
                .align(SWT.FILL, SWT.FILL).applyTo(label);

        Text title = new Text(this, SWT.WRAP);
        title.setEditable(false);
        title.setText(notification.getTitle());
        titleFont = Utils.createFont(18, SWT.NORMAL, Utils.HELVETICA,
                Utils.ARIAL);
        title.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_ARROW));

        footerFont = Utils.createFont(12, SWT.BOLD, Utils.HELVETICA,
                Utils.ARIAL);

        footerColor = new Color(getDisplay(), 0, 180, 240);

        title.setFont(titleFont);

        GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
                .indent(10, SWT.DEFAULT).align(SWT.FILL, SWT.FILL)
                .applyTo(title);

        Control description = createDescription();
        GridDataFactory.fillDefaults().grab(true, true).span(2, 1)
                .indent(10, SWT.DEFAULT).align(SWT.FILL, SWT.FILL)
                .applyTo(description);

        NotificationHyperlink readMore = new NotificationHyperlink(this);
        GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER)
                .indent(LEFT_MARGIN, SWT.DEFAULT).applyTo(readMore);
        readMore.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
        readMore.setFont(footerFont);
        readMore.setText(Messages.Notifications_NotificationInfoReadMore);

        NotificationHyperlink link = new NotificationHyperlink(this);
        GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).indent(-5, 0)
                .applyTo(link);
        link.registerMouseTrackListener();
        link.setFont(footerFont);
        link.setText(notification.getLinkText() + Utils.FORWARD_SUFFIX);
        link.setForeground(footerColor);
        link.addHyperlinkListener(listener);
    }

    /**
     * Unfortunately REST API is not really Web independent API. It contains
     * HTML tags which means it's suggested to be rendered by browser.
     * 
     * This method uses SWT browser component if it's available and uses
     * non-editable Text with striped HTML tags from the text as a fallback (the
     * result loses styling of course, so it may look ugly).
     */
    private Control createDescription() {
        if (ContributionService.getInstance().isEmbeddedBrowserAvailable()) {
            return createBrowserDescription();
        } else {
            return createTextDescription();
        }
    }

    private Control createBrowserDescription() {
        Browser browser = new Browser(this, SWT.NO_FOCUS);
        StringBuilder builder = new StringBuilder(HTML_PREFIX);
        builder.append(notification.getDescription());
        builder.append(HTML_SUFFIX);
        browser.setText(builder.toString());
        browser.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
        return browser;
    }

    private Control createTextDescription() {
        Text description = new Text(this, SWT.NO_FOCUS | SWT.WRAP);
        description.setTouchEnabled(false);
        description.setEditable(false);

        description.setText(getStrippedHtmlDescription());

        labelsFont = new Font(getDisplay(), Utils
                .getModifiedFontData(description.getFont().getFontData(), 12));

        description.setFont(labelsFont);
        return description;
    }

    private String getStrippedHtmlDescription() {
        String descr = notification.getDescription();

        XMLReader reader = new Parser();
        reader.setContentHandler(listener);
        listener.reset();
        InputSource source = new InputSource(new StringReader(descr));

        String result = descr;
        try {
            reader.parse(source);
            result = listener.getStrippedHtml();
        } catch (IOException e) {
            LOG.log(Level.WARNING, STRIP_HTML_ERROR + descr, e);
        } catch (SAXException e) {
            LOG.log(Level.WARNING, STRIP_HTML_ERROR + descr, e);
        }

        listener.reset();
        return result;
    }

    private class Listener extends HyperlinkAdapter
            implements DisposeListener, ContentHandler {

        private StringBuilder builder;

        public void widgetDisposed(DisposeEvent e) {
            if (titleFont != null) {
                titleFont.dispose();
                titleFont = null;
                footerFont.dispose();
                footerFont = null;
                footerColor.dispose();
                footerColor = null;
            }
            if (labelsFont != null) {
                labelsFont.dispose();
                labelsFont = null;
            }
        }

        public void reset() {
            builder = new StringBuilder();
        }

        public String getStrippedHtml() {
            return builder.toString();
        }

        @Override
        public void linkActivated(HyperlinkEvent e) {
            ContributionService.getInstance()
                    .notificationLaunched(notification);
            if (Program.launch(notification.getLink())) {
                manager.close();
            }
        }

        public void characters(char[] ch, int start, int length)
                throws SAXException {
            for (int idx = 0; idx < length; idx++) {
                builder.append(ch[idx + start]);
            }
        }

        public void ignorableWhitespace(char[] ch, int start, int length)
                throws SAXException {
            builder.append(ch);
        }

        public void setDocumentLocator(Locator locator) {
        }

        public void startDocument() throws SAXException {
        }

        public void endDocument() throws SAXException {
        }

        public void startPrefixMapping(String prefix, String uri)
                throws SAXException {
        }

        public void endPrefixMapping(String prefix) throws SAXException {
        }

        public void startElement(String uri, String localName, String qName,
                Attributes atts) throws SAXException {
            if (localName.equals(BR) || localName.equals(P)) {
                builder.append(NEW_LINE);
            } else if (localName.equals(PRE)) {
                builder.append(NEW_LINE).append(NEW_LINE);
            }
        }

        public void endElement(String uri, String localName, String qName)
                throws SAXException {
            if (localName.equals(P)) {
                builder.append(NEW_LINE);
            } else if (localName.equals(PRE)) {
                builder.append(NEW_LINE).append(NEW_LINE);
            }
        }

        public void processingInstruction(String target, String data)
                throws SAXException {
        }

        public void skippedEntity(String name) throws SAXException {
        }

    }
}
