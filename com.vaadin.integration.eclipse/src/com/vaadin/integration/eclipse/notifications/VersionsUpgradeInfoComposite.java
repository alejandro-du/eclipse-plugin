package com.vaadin.integration.eclipse.notifications;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;

import com.vaadin.integration.eclipse.notifications.model.VersionUpdateNotification;
import com.vaadin.integration.eclipse.util.ProjectUtil;
import com.vaadin.integration.eclipse.util.data.AbstractVaadinVersion;
import com.vaadin.integration.eclipse.util.data.MavenVaadinVersion;

/**
 * Composite which shows full info about notification.
 *
 */
class VersionsUpgradeInfoComposite extends Composite {

    private final VersionUpdateNotification notification;

    private final PopupUpdateManager manager;

    private Font titleFont;

    private final Listener listener;

    private Font labelsFont;

    private Font footerFont;

    private Color footerColor;

    public VersionsUpgradeInfoComposite(Composite parent,
            VersionUpdateNotification notification,
            PopupUpdateManager manager) {
        super(parent, SWT.NONE);
        this.notification = notification;
        this.manager = manager;

        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.marginBottom = 10;
        layout.verticalSpacing = 5;
        setLayout(layout);

        listener = new Listener();

        doSetBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));

        addDisposeListener(new Listener());

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
        GridDataFactory.fillDefaults().grab(true, false)
                .align(SWT.FILL, SWT.FILL).applyTo(label);

        Text title = new Text(this, SWT.WRAP);
        title.setEditable(false);
        title.setText(Messages.Notifications_VersionsTitle);
        titleFont = Utils.createFont(18, SWT.NORMAL, Utils.HELVETICA,
                Utils.ARIAL);
        title.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_ARROW));

        footerFont = Utils.createFont(12, SWT.BOLD, Utils.HELVETICA,
                Utils.ARIAL);

        footerColor = new Color(getDisplay(), 0, 180, 240);

        title.setFont(titleFont);

        GridDataFactory.fillDefaults().grab(true, false).indent(5, 10)
                .align(SWT.FILL, SWT.FILL).applyTo(title);

        Control description = createDescription();
        GridDataFactory.fillDefaults().grab(true, true)
                .align(SWT.FILL, SWT.FILL).applyTo(description);

        NotificationHyperlink link = new NotificationHyperlink(this);
        GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).indent(10, 0)
                .applyTo(link);
        link.registerMouseTrackListener();
        link.setFont(footerFont);
        link.setText(Messages.Notifications_VersionsDismiss);
        link.setForeground(footerColor);
        link.addHyperlinkListener(listener);
    }

    private Control createDescription() {
        ScrolledComposite scrolled = new ScrolledComposite(this,
                SWT.NO_FOCUS | SWT.V_SCROLL);

        scrolled.setExpandHorizontal(true);
        scrolled.setExpandVertical(true);

        Composite composite = new Composite(scrolled, SWT.NO_FOCUS);

        GridLayout layout = new GridLayout(1, false);
        layout.horizontalSpacing = 0;
        layout.marginLeft = 5;
        layout.marginRight = 5;
        composite.setLayout(layout);

        scrolled.setContent(composite);

        labelsFont = Utils.createFont(12, SWT.NORMAL, Utils.HELVETICA,
                Utils.ARIAL);

        boolean hasNightlies = makeNightlySection(composite);
        makeUpgradeSection(composite, hasNightlies);

        scrolled.setMinSize(scrolled.computeSize(0, SWT.DEFAULT));
        return scrolled;
    }

    private boolean makeNightlySection(Composite composite) {
        Map<IProject, ? extends AbstractVaadinVersion> nightlies = notification
                .getNightlyUpgrades();
        if (!nightlies.isEmpty()) {
            Label label = new Label(composite, SWT.NONE);
            label.setText(Messages.Notifications_VersionsNightlyBuildsSection);
            label.setFont(footerFont);
            GridDataFactory.fillDefaults().grab(true, false)
                    .align(SWT.FILL, SWT.FILL).applyTo(label);
            for (Entry<IProject, ? extends AbstractVaadinVersion> entry : nightlies
                    .entrySet()) {
                Label nightly = new Label(composite, SWT.NONE);
                nightly.setText(MessageFormat.format(
                        Messages.Notifications_VersionsNightlyProjectItem,
                        entry.getKey().getName(), entry.getValue().toString()));
                nightly.setFont(labelsFont);
            }
            return true;
        }
        return false;
    }

    private void makeUpgradeSection(Composite composite,
            boolean hasNightlySection) {
        Map<IProject, List<MavenVaadinVersion>> upgrades = notification
                .getUpgrades();
        if (!upgrades.isEmpty()) {
            Label label = new Label(composite, SWT.NONE);
            label.setText(Messages.Notifications_VersionsVaadin7ProjectsTitle);
            GridDataFactory factory = GridDataFactory.fillDefaults()
                    .grab(true, false).align(SWT.FILL, SWT.FILL);
            if (hasNightlySection) {
                factory.indent(0, 10);
            }
            factory.applyTo(label);
            label.setFont(footerFont);
            for (Entry<IProject, List<MavenVaadinVersion>> entry : upgrades
                    .entrySet()) {
                String currentVersion = getProjectVersion(entry.getKey());
                Label versionedProject = new Label(composite, SWT.NONE);
                versionedProject.setFont(labelsFont);
                int versionLength;
                if (currentVersion == null || currentVersion.isEmpty()) {
                    versionLength = 0;
                } else {
                    versionLength = currentVersion.length();
                }
                versionedProject.setText(MessageFormat.format(
                        Messages.Notifications_VersionsVaadin7ProjectItem,
                        entry.getKey().getName(), currentVersion,
                        versionLength));

                createVersionsControl(composite, entry.getValue());
            }
        }
    }

    private void createVersionsControl(Composite composite,
            List<MavenVaadinVersion> versions) {
        StyledText text = new StyledText(composite, SWT.NO_FOCUS | SWT.WRAP);
        text.setEditable(false);
        text.setFont(labelsFont);
        text.setCaret(null);
        text.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_ARROW));

        List<StyleRange> ranges = new ArrayList<StyleRange>(versions.size());
        text.setText(buildVersionsString(versions, ranges));
        text.setStyleRanges(ranges.toArray(new StyleRange[ranges.size()]));

        GridDataFactory.fillDefaults().grab(true, false)
                .align(SWT.FILL, SWT.FILL).indent(10, -5).applyTo(text);
    }

    private String buildVersionsString(List<MavenVaadinVersion> versions,
            List<StyleRange> ranges) {
        StringBuilder builder = new StringBuilder(MessageFormat.format(
                Messages.Notifications_VersionsVaadin7AvailableVersions,
                versions.size()));
        for (MavenVaadinVersion version : versions) {
            int index = builder.length();
            builder.append(version.toString());
            builder.append(',').append(' ');
            StyleRange styleRange = new StyleRange();
            styleRange.start = index;
            styleRange.length = version.toString().length();
            styleRange.foreground = footerColor;
            ranges.add(styleRange);
        }
        if (builder.length() > 0) {
            builder.delete(builder.length() - 2, builder.length());
        }
        return builder.toString();
    }

    private String getProjectVersion(IProject project) {
        try {
            return ProjectUtil.getVaadinLibraryVersion(project, true);
        } catch (CoreException e) {
            Logger.getLogger(VersionsUpgradeInfoComposite.class.getName())
                    .log(Level.WARNING, null, e);
        }
        return null;
    }

    private class Listener extends HyperlinkAdapter implements DisposeListener {

        public void widgetDisposed(DisposeEvent e) {
            if (titleFont != null) {
                titleFont.dispose();
                titleFont = null;
                footerFont.dispose();
                footerFont = null;
                footerColor.dispose();
                footerColor = null;
                labelsFont.dispose();
                labelsFont = null;
            }
        }

        @Override
        public void linkActivated(HyperlinkEvent e) {
            manager.dismissVersionNotification();
        }

    }
}
