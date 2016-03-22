package com.vaadin.integration.eclipse.preferences;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.mylyn.commons.ui.compatibility.CommonFonts;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;

import com.vaadin.integration.eclipse.VaadinPlugin;

/**
 * The Eclipse preferences page for Vaadin plugin.
 */

public class VaadinPreferences extends PreferencePage
        implements IWorkbenchPreferencePage {

    private final List<VaadinFieldEditor> editors;

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

        createNotificationsSection(composite);

        createMavenSection(composite);

        createPrereleaseSection(composite);

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
                updateNotificationContols(enabled);
            }

        });

        addField(new VaadinBooleanFieldEditor(
                PreferenceConstants.NOTIFICATIONS_CENTER_POPUP_ENABLED,
                Messages.VaadinPreferences_NotificationsPopup, panel, true));

        createUpdateSection(panel);

        updateNotificationContols(enabled);
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
                "Enable Vaadin pre-release archetypes", panel, true);
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
                "Enable automatic widgetset compilation", panel, true);
        addField(autoWidgetsetBuildEnabled);
    }

    private void createUpdateSection(Composite panel) {
        Group group = new Group(panel, SWT.FILL);

        GridData data = new GridData();
        data.verticalIndent = 10;
        data.horizontalAlignment = SWT.FILL;
        group.setLayoutData(data);
        data.grabExcessHorizontalSpace = true;

        group.setText(Messages.VaadinPreferences_UpdateSchedule);
        VaadinBooleanFieldEditor editor = new VaadinBooleanFieldEditor(
                PreferenceConstants.NOTIFICATIONS_FETCH_ON_OPEN,
                Messages.VaadinPreferences_NotificationsFetchOnOpen, group,
                true);
        ((GridData) editor.getControl().getLayoutData()).horizontalSpan = 2;
        addField(editor);

        editor = new VaadinBooleanFieldEditor(
                PreferenceConstants.NOTIFICATIONS_FETCH_ON_START,
                Messages.VaadinPreferences_NotificationsFetchOnStart, group,
                true);
        ((GridData) editor.getControl().getLayoutData()).horizontalSpan = 2;
        addField(editor);

        addField(new NotificationsComboFieldEditor(
                PreferenceConstants.NOTIFICATIONS_CENTER_POLLING_INTERVAL,
                Messages.VaadinPreferences_NotificationsPollingInterval,
                group));
        addField(new NotificationsComboFieldEditor(
                PreferenceConstants.NOTIFICATIONS_NEW_VERSION_POLLING_INTERVAL,
                Messages.VaadinPreferences_NotificationsVersionPollingInterval,
                group));

        GridLayout groupLayout = new GridLayout(2, false);
        groupLayout.marginTop = 5;
        groupLayout.marginBottom = 5;
        group.setLayout(groupLayout);

    }

    private void updateNotificationContols(
            final VaadinBooleanFieldEditor enabled) {
        for (VaadinFieldEditor editor : editors) {
            if (!editor.equals(enabled) && editor.isNotificationEditor()) {
                editor.setEnable(enabled.getBooleanValue());
            }
        }
    }

    private interface VaadinFieldEditor {
        void store();

        void setPresentsDefaultValue(boolean booleanValue);

        void setEnable(boolean enable);

        boolean isNotificationEditor();
    }

    private static class VaadinBooleanFieldEditor extends BooleanFieldEditor
            implements VaadinFieldEditor {

        private Button control;
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

    private static class NotificationsComboFieldEditor extends ComboFieldEditor
            implements VaadinFieldEditor {

        private Combo control;

        public NotificationsComboFieldEditor(String prefName, String label,
                Composite parent) {
            super(prefName, label, getOptions(), parent);
        }

        public void setEnable(boolean enable) {
            getLabelControl().setEnabled(enable);
            control.setEnabled(enable);
        }

        public boolean isNotificationEditor() {
            return true;
        }

        @Override
        protected void doFillIntoGrid(Composite parent, int numColumns) {
            int size = parent.getChildren().length;
            super.doFillIntoGrid(parent, numColumns);
            parent.getChildren()[size].setEnabled(false);
            control = (Combo) parent.getChildren()[size + 1];
            GridData data = ((GridData) control.getLayoutData());
            data.horizontalAlignment = SWT.RIGHT;
            data.grabExcessHorizontalSpace = true;
        }

        @Override
        public void setPresentsDefaultValue(boolean booleanValue) {
            super.setPresentsDefaultValue(booleanValue);
        }

        private static String[][] getOptions() {
            String[][] values = new String[NotificationsPollingSchedule
                                           .values().length][];
            values[0] = new String[] { Messages.VaadinPreferences_OncePerHour,
                    String.valueOf(NotificationsPollingSchedule.PER_HOUR
                            .getSeconds()) };

            values[1] = new String[] { Messages.VaadinPreferences_OncePer4Hours,
                    String.valueOf(NotificationsPollingSchedule.PER_FOUR_HOUR
                            .getSeconds()) };

            values[2] = new String[] { Messages.VaadinPreferences_OncePerDay,
                    String.valueOf(NotificationsPollingSchedule.PER_DAY
                            .getSeconds()) };

            values[3] = new String[] { Messages.VaadinPreferences_Never,
                    String.valueOf(NotificationsPollingSchedule.NEVER
                            .getSeconds()) };
            return values;
        }
    }

    private static final class ExpansionListener extends ExpansionAdapter {
        @Override
        public void expansionStateChanged(ExpansionEvent e) {
            ((Control) e.getSource()).getParent().layout();
        }
    }

}