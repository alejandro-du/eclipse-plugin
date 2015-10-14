package com.vaadin.integration.eclipse.notifications;

import java.text.MessageFormat;
import java.util.logging.Logger;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;

import com.vaadin.integration.eclipse.VaadinPlugin;

/**
 * Composite widget which accept user token to access notifications.
 *
 */
class TokenInputComposite extends Composite {

    private final PopupUpdateManager manager;

    private StyledText token;

    private Label wrongTokenLabel;
    private Color errorColor;
    private Font font;
    private Font inputFont;
    private Color textColor;
    private Color linkColor;

    private static final int VERTICAL_SPACE = 5;

    private final Listener listener;

    TokenInputComposite(Composite parent, PopupUpdateManager updateManager) {
        super(parent, SWT.NONE);

        manager = updateManager;
        listener = new Listener();

        parent.getDisplay().addFilter(SWT.Traverse, listener);

        GridLayout layout = new GridLayout(2, false);
        layout.verticalSpacing = 7;
        layout.marginRight = 5;
        layout.marginLeft = 5;
        setLayout(layout);

        initComponents();
    }

    private void initComponents() {
        font = Utils.createFont(12, SWT.NORMAL, Utils.HELVETICA, Utils.ARIAL);
        inputFont = Utils.createFont(14, SWT.NORMAL, Utils.HELVETICA,
                Utils.ARIAL);
        textColor = new Color(getDisplay(), 77, 77, 77);
        linkColor = new Color(getDisplay(), 0, 180, 240);
        errorColor = new Color(getDisplay(), 181, 3, 3);

        createSteps();

        Label label = new Label(this, SWT.NONE);
        label.setText(Messages.Notifications_TokenViewTitle);
        GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
                .indent(0, 10).align(SWT.FILL, SWT.TOP).applyTo(label);
        label.setFont(font);
        label.setForeground(textColor);

        token = new StyledText(this, SWT.PASSWORD | SWT.BORDER | SWT.SINGLE);
        token.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
        int topMargin = 10;
        GridDataFactory.fillDefaults().grab(true, false)
                .align(SWT.FILL, SWT.TOP)
                .hint(SWT.DEFAULT, Utils.FIELD_HEIGHT - topMargin).span(2, 1)
                .applyTo(token);
        token.setTopMargin(10);
        token.setLeftMargin(10);
        token.setFont(inputFont);

        Label button = new Label(this, SWT.NONE);
        Image image = VaadinPlugin.getInstance().getImageRegistry()
                .get(Utils.SUBMIT_BUTTON);
        button.setBackgroundImage(image);
        GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.TOP).indent(0, 10)
                .span(2, 1)
                .hint(image.getImageData().width, image.getImageData().height)
                .applyTo(button);

        button.addMouseListener(listener);
        button.addMouseTrackListener(listener);
    }

    private void createSteps() {
        int i = 1;
        createItemNumber(i, false);

        createFirstItemText();

        i++;
        createItemNumber(i, true);

        createItemText(Messages.Notifications_TokenDescriptionItem2);

        i++;
        createItemNumber(i, true);
        createItemText(Messages.Notifications_TokenDescriptionItem3);

        i++;
        createItemNumber(i, true);
        createItemText(Messages.Notifications_TokenDescriptionItem4);

    }

    private void createFirstItemText() {
        StyledText text = new StyledText(this, SWT.WRAP);
        text.setEditable(false);
        String msg = Messages.Notifications_TokenDescriptionItem1;
        String vaadin = Messages.Notifications_TokenDescriptionVaadin;

        text.setText(MessageFormat.format(msg, vaadin));

        int index = msg.indexOf(Utils.FIRST_POSITION);
        if (index != -1) {
            StyleRange styleRange = new StyleRange();
            styleRange.start = index;
            styleRange.length = vaadin.length();
            styleRange.foreground = linkColor;
            styleRange.underlineStyle = SWT.UNDERLINE_LINK;
            text.setStyleRange(styleRange);

            LinkListener linkListener = new LinkListener(index,
                    index + vaadin.length());
            text.addMouseListener(linkListener);
            text.addMouseMoveListener(linkListener);
            text.addMouseTrackListener(linkListener);
        }
        text.setFont(font);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL)
                .grab(true, false).applyTo(text);
        text.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
        text.setCaret(null);
    }

    private void createItemText(String text) {
        // use styled text everywhere because of spaces (between components )
        // and different baselines
        StyledText item = new StyledText(this, SWT.WRAP);
        item.setEditable(false);
        item.setText(text);
        item.setFont(font);
        item.setCaret(null);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL)
                .grab(true, false).indent(0, VERTICAL_SPACE).applyTo(item);
        item.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
    }

    private void createItemNumber(int i, boolean indent) {
        // use styled text everywhere because of spaces (between components )
        // and different baselines
        StyledText label = new StyledText(this, SWT.NO_FOCUS);
        label.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
        String item = Messages.Notifications_TokenItemNumber;
        label.setText(MessageFormat.format(item, i));
        label.setFont(font);
        label.setEditable(false);
        label.setForeground(textColor);
        label.setCaret(null);
        GridDataFactory factory = GridDataFactory.fillDefaults().align(SWT.LEFT,
                SWT.FILL);
        if (indent) {
            factory.indent(0, VERTICAL_SPACE);
        }
        factory.applyTo(label);
    }

    private static class LinkListener extends MouseTrackAdapter
            implements MouseMoveListener, MouseListener {

        private final int startPosition;
        private final int endPosition;

        private boolean isLinkActive;

        LinkListener(int startPos, int endPos) {
            startPosition = startPos;
            endPosition = endPos;
        }

        public void mouseDoubleClick(MouseEvent e) {
            if (isLinkTarget(e)) {
                activateLink();
            }
        }

        public void mouseUp(MouseEvent e) {
            if (isLinkTarget(e)) {
                activateLink();
            }
        }

        public void mouseDown(MouseEvent e) {
            // handle only mouse up event
        }

        @Override
        public void mouseExit(MouseEvent e) {
            unhover(e);
        }

        public void mouseMove(MouseEvent e) {
            handleLinkHover(e);
        }

        private void handleLinkHover(MouseEvent e) {
            if (isLinkTarget(e)) {
                if (!isLinkActive) {
                    isLinkActive = true;
                    underlineLink(true, getTextWidget(e));
                }
            } else if (isLinkActive) {
                unhover(e);
            }
        }

        private void unhover(MouseEvent e) {
            isLinkActive = false;
            underlineLink(false, getTextWidget(e));
        }

        private void underlineLink(boolean underline, StyledText text) {
            StyleRange styleRange = text.getStyleRanges()[0];
            styleRange.underline = underline;

            text.setStyleRange(null);
            text.setStyleRange(styleRange);
        }

        private void activateLink() {
            if (!Program.launch(Utils.SIGN_IN_URL)) {
                Logger.getLogger(TokenInputComposite.class.getName())
                        .warning("Couldn't open sign in URL "
                                + Utils.SIGN_IN_URL + " in external browsrer");
            }
        }

        private boolean isLinkTarget(MouseEvent e) {
            Rectangle bounds = getTextWidget(e).getTextBounds(startPosition,
                    endPosition);
            return bounds.contains(new Point(e.x, e.y));
        }

        private StyledText getTextWidget(MouseEvent e) {
            return (StyledText) e.widget;
        }

    }

    private class Listener extends HyperlinkAdapter
            implements DisposeListener, MouseListener, MouseTrackListener,
            org.eclipse.swt.widgets.Listener {

        public void widgetDisposed(DisposeEvent e) {
            if (font != null) {
                font.dispose();
                font = null;
                inputFont.dispose();
                inputFont = null;
                textColor.dispose();
                textColor = null;
                linkColor.dispose();
                linkColor = null;
            }
            if (errorColor != null) {
                errorColor.dispose();
                errorColor = null;
            }
            Display.getCurrent().removeFilter(SWT.Traverse, this);
        }

        public void handleEvent(Event event) {
            if (SWT.TRAVERSE_RETURN == event.detail) {
                if (event.widget == TokenInputComposite.this) {
                    handleClick();
                }
            }
        }

        @Override
        public void linkActivated(HyperlinkEvent e) {
        }

        public void mouseDoubleClick(MouseEvent e) {
            Label button = (Label) e.widget;
            button.setImage(VaadinPlugin.getInstance().getImageRegistry()
                    .get(Utils.SIGN_IN_PRESSED_BUTTON));
            handleClick();
        }

        public void mouseDown(MouseEvent e) {
            final Label button = (Label) e.widget;
            button.setBackgroundImage(VaadinPlugin.getInstance()
                    .getImageRegistry().get(Utils.SUBMIT_PRESSED_BUTTON));
        }

        public void mouseUp(MouseEvent e) {
            Label button = (Label) e.widget;
            button.setBackgroundImage(VaadinPlugin.getInstance()
                    .getImageRegistry().get(Utils.SUBMIT_BUTTON));
            handleClick();
        }

        public void mouseEnter(MouseEvent e) {
            Label button = (Label) e.widget;
            button.setBackgroundImage(VaadinPlugin.getInstance()
                    .getImageRegistry().get(Utils.SUBMIT_HOVER_BUTTON));
        }

        public void mouseExit(MouseEvent e) {
            Label button = (Label) e.widget;
            button.setBackgroundImage(VaadinPlugin.getInstance()
                    .getImageRegistry().get(Utils.SUBMIT_BUTTON));
        }

        public void mouseHover(MouseEvent e) {
        }

        private void handleClick() {
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()
                    .forceActive();
            String text = token.getText().trim();
            if (ContributionService.getInstance().validateToken(text)) {
                manager.showNotificationsList();
            } else if (wrongTokenLabel == null
                    || wrongTokenLabel.isDisposed()) {
                wrongTokenLabel = new Label(TokenInputComposite.this, SWT.NONE);
                wrongTokenLabel.setForeground(errorColor);
                wrongTokenLabel.setText(Messages.Notifications_TokenErrorMsg);
                wrongTokenLabel.setFont(font);

                GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
                        .indent(0, 10).align(SWT.FILL, SWT.TOP)
                        .applyTo(wrongTokenLabel);
                layout();
            }
        }

    }
}
