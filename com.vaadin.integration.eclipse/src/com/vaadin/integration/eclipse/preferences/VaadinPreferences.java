package com.vaadin.integration.eclipse.preferences;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.mylyn.commons.ui.compatibility.CommonFonts;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.notifications.ContributionService;

/**
 * The Eclipse preferences page for Vaadin plugin.
 */

public class VaadinPreferences extends PreferencePage
        implements IWorkbenchPreferencePage {

    private final List<VaadinFieldEditor> editors;
    private Button signOutButton;

    public VaadinPreferences() {
        setPreferenceStore(VaadinPlugin.getInstance().getPreferenceStore());
        editors = new ArrayList<VaadinFieldEditor>();
    }

    public void init(IWorkbench workbench) {
    }

    @Override
    public boolean performOk() {
        for (VaadinFieldEditor editor : editors) {
            editor.store();
            editor.setPresentsDefaultValue(false);
        }
        return true;
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        composite.setLayout(layout);
        composite.setFont(parent.getFont());

        createMavenSection(composite);

        createPrereleaseSection(composite);

        createNotificationsSection(composite);

        // checkState();
        return composite;
    }

    private <T extends FieldEditor & VaadinFieldEditor> void addField(
            T editor) {
        editors.add(editor);
        editor.setPage(this);
        editor.setPreferenceStore(getPreferenceStore());
        editor.load();
    }

    private void createNotificationsSection(Composite composite) {
        final ExpandableComposite expandable = new ExpandableComposite(
                composite, SWT.FILL, ExpandableComposite.TWISTIE
                        | ExpandableComposite.CLIENT_INDENT);
        expandable.setExpanded(true);
        GridData data = new GridData();
        data.horizontalAlignment = SWT.FILL;
        data.grabExcessHorizontalSpace = true;
        expandable.setLayoutData(data);

        expandable.addExpansionListener(new ExpansionListener());

        Composite panel = new Composite(expandable, SWT.NONE);
        expandable.setClient(panel);
        panel.setLayout(new GridLayout(1, false));
        expandable
                .setText(Messages.VaadinPreferences_NotificationsSectionTitle);
        expandable.setFont(CommonFonts.BOLD);

        final VaadinBooleanFieldEditor enabled = new VaadinBooleanFieldEditor(
                PreferenceConstants.NOTIFICATIONS_ENABLED,
                Messages.VaadinPreferences_NotificationsEnable, panel, true);
        addField(enabled);
        enabled.getControl().addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                updateNotificationControls(enabled);
            }

        });

        addField(new VaadinBooleanFieldEditor(
                PreferenceConstants.NOTIFICATIONS_CENTER_POPUP_ENABLED,
                Messages.VaadinPreferences_NotificationsPopup, panel, true));

        addField(new VaadinStringCheckboxEditor(
                PreferenceConstants.NOTIFICATIONS_NEW_VERSION_POLLING_INTERVAL,
                Messages.VaadinPreferences_VersionNotifications, panel, true,
                String.valueOf(NotificationsPollingSchedule.PER_DAY
                        .getSeconds()),
                String.valueOf(NotificationsPollingSchedule.NEVER.getSeconds())));
        addField(new VaadinStringCheckboxEditor(
                PreferenceConstants.NOTIFICATIONS_CENTER_POLLING_INTERVAL,
                Messages.VaadinPreferences_OtherNotifications, panel, true,
                String.valueOf(NotificationsPollingSchedule.PER_FOUR_HOUR
                        .getSeconds()),
                String.valueOf(NotificationsPollingSchedule.NEVER.getSeconds())));

        signOutButton = new Button(panel, SWT.PUSH);
        signOutButton
                .setText(com.vaadin.integration.eclipse.notifications.Messages.Notifications_SignOut);
        signOutButton.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                ContributionService.getInstance().signOut(null);
                updateNotificationControls(enabled);
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                ContributionService.getInstance().signOut(null);
                updateNotificationControls(enabled);
            }
        });

        updateNotificationControls(enabled);
    }

    private void createPrereleaseSection(Composite composite) {
        final ExpandableComposite expandable = new ExpandableComposite(
                composite, SWT.FILL, ExpandableComposite.TWISTIE
                        | ExpandableComposite.CLIENT_INDENT);
        expandable.setExpanded(false);
        GridData data = new GridData();
        data.horizontalAlignment = SWT.FILL;
        data.grabExcessHorizontalSpace = true;
        expandable.setLayoutData(data);

        expandable.addExpansionListener(new ExpansionListener());

        Composite panel = new Composite(expandable, SWT.NONE);
        expandable.setClient(panel);
        panel.setLayout(new GridLayout(1, false));
        expandable.setText("Vaadin Pre-releases");
        expandable.setFont(CommonFonts.BOLD);

        final VaadinBooleanFieldEditor enabled = new VaadinBooleanFieldEditor(
                PreferenceConstants.PRERELEASE_ARCHETYPES_ENABLED,
                "Enable Vaadin pre-release archetypes", panel, false);
        addField(enabled);
    }

    private void createMavenSection(Composite composite) {
        final ExpandableComposite expandable = new ExpandableComposite(
                composite, SWT.FILL, ExpandableComposite.TWISTIE
                        | ExpandableComposite.CLIENT_INDENT);
        expandable.setExpanded(true);
        GridData data = new GridData();
        data.horizontalAlignment = SWT.FILL;
        data.grabExcessHorizontalSpace = true;
        expandable.setLayoutData(data);

        expandable.addExpansionListener(new ExpansionListener());

        Composite panel = new Composite(expandable, SWT.NONE);
        expandable.setClient(panel);
        panel.setLayout(new GridLayout(1, false));
        expandable.setText("Maven");
        expandable.setFont(CommonFonts.BOLD);

        final VaadinBooleanFieldEditor autoWidgetsetBuildEnabled = new VaadinBooleanFieldEditor(
                PreferenceConstants.MAVEN_WIDGETSET_AUTOMATIC_BUILD_ENABLED,
                "Enable automatic widgetset compilation", panel, false);
        addField(autoWidgetsetBuildEnabled);
    }

    private void updateNotificationControls(
            final VaadinBooleanFieldEditor enableControl) {
        boolean enabled = enableControl.getBooleanValue();
        for (VaadinFieldEditor editor : editors) {
            if (!editor.equals(enableControl) && editor.isNotificationEditor()) {
                editor.setEnable(enabled);
            }
        }
        signOutButton.setEnabled(enabled
                && ContributionService.getInstance().isSignedIn());
    }

    private interface VaadinFieldEditor {
        void store();

        void setPresentsDefaultValue(boolean booleanValue);

        void setEnable(boolean enable);

        boolean isNotificationEditor();
    }

    private static class VaadinBooleanFieldEditor extends BooleanFieldEditor
            implements VaadinFieldEditor {

        protected Button control;
        private boolean isNotificationEditor;

        VaadinBooleanFieldEditor(String name, String label, Composite composite,
                boolean notificationRelated) {
            super(name, label, composite);
            isNotificationEditor = notificationRelated;
        }

        @Override
        public void setPresentsDefaultValue(boolean booleanValue) {
            super.setPresentsDefaultValue(booleanValue);
        }

        public void setEnable(boolean enable) {
            getControl().setEnabled(enable);
        }

        public boolean isNotificationEditor() {
            return isNotificationEditor;
        }

        @Override
        protected Button getChangeControl(Composite parent) {
            control = super.getChangeControl(parent);
            return control;
        }

        private Button getControl() {
            return control;
        }
    }

    private static class VaadinStringCheckboxEditor extends
            VaadinBooleanFieldEditor {
        private String trueValue;
        private String falseValue;

        public VaadinStringCheckboxEditor(String name, String label,
                Composite composite, boolean notificationRelated,
                String trueValue, String falseValue) {
            super(name, label, composite, notificationRelated);
            this.trueValue = trueValue;
            this.falseValue = falseValue;
        }

        @Override
        protected void doStore() {
            String value = getBooleanValue() ? trueValue : falseValue;
            getPreferenceStore().setValue(getPreferenceName(), value);
        }

        @Override
        protected void doLoad() {
            super.doLoadDefault();
            String value = getPreferenceStore().getString(
                    getPreferenceName());
            if (control != null) {
                control.setSelection(!falseValue.equals(value));
            }
        }
    }

    private static final class ExpansionListener extends ExpansionAdapter {
        @Override
        public void expansionStateChanged(ExpansionEvent e) {
            ((Control) e.getSource()).getParent().layout();
        }
    }

}