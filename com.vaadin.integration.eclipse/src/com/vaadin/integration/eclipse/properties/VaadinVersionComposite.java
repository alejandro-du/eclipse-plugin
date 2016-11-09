package com.vaadin.integration.eclipse.properties;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.PreferenceUtil;
import com.vaadin.integration.eclipse.util.ProjectUtil;
import com.vaadin.integration.eclipse.util.VersionUtil;
import com.vaadin.integration.eclipse.util.data.AbstractVaadinVersion;
import com.vaadin.integration.eclipse.util.data.LocalVaadinVersion;
import com.vaadin.integration.eclipse.util.data.MavenVaadinVersion;
import com.vaadin.integration.eclipse.util.files.LocalFileManager;
import com.vaadin.integration.eclipse.util.files.LocalFileManager.FileType;
import com.vaadin.integration.eclipse.util.network.MavenVersionManager;

/**
 * Project property page for selecting an Vaadin version and updating the JAR in
 * the project.
 */
public class VaadinVersionComposite extends Composite {

    private Combo versionCombo;
    // Local or Maven versions
    private Map<String, AbstractVaadinVersion> versionMap = new HashMap<String, AbstractVaadinVersion>();
    private IProject project = null;
    private Button updateNotificationCheckbox;
    // by default, do not allow selecting Vaadin 7 versions
    private boolean useDependencyManagement = false;

    public VaadinVersionComposite(Composite parent, int style) {
        super(parent, style);

        setLayout(new GridLayout(3, false));
        GridData data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        setLayoutData(data);
    }

    private void addVersionSelectionSection() {
        Label label = new Label(this, SWT.NULL);
        label.setText("Vaadin version:");

        // Vaadin version selection combo
        versionCombo = new Combo(this,
                SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        versionCombo.setLayoutData(gd);

        // Add a checkbox for choosing whether to notify the user of Vaadin
        // version updates.
        updateNotificationCheckbox = new Button(this, SWT.CHECK);
        updateNotificationCheckbox.setText("Notify of new Vaadin versions");
        updateNotificationCheckbox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean newValue = updateNotificationCheckbox.getSelection();
                if (project == null) {
                    return;
                }
                PreferenceUtil prefUtil = PreferenceUtil.get(project);
                prefUtil.setUpdateNotificationEnabled(newValue);
            }
        });
        GridData gridData = new GridData(GridData.FILL, GridData.BEGINNING,
                true, false);
        gridData.horizontalSpan = 3;
        updateNotificationCheckbox.setLayoutData(gridData);

        getShell().layout(false);
    }

    private void setControlVisible(Control control, boolean visible) {
        GridData data = (GridData) control.getLayoutData();
        data.exclude = !visible;
        control.setVisible(visible);
    }

    private void updateVersionCombo() {
        versionCombo.setEnabled(true);
        try {

            versionCombo.removeAll();
            // Always allow empty selection which removes Vaadin from the
            // project
            versionCombo.add("");
            versionMap.clear();

            // Vaadin 7 (dependency management) or older versions
            if (useDependencyManagement) {
                for (MavenVaadinVersion version : MavenVersionManager
                        .getAvailableVersions(false)) {
                    versionMap.put(version.getVersionNumber(), version);
                    versionCombo.add(version.getVersionNumber());
                }
            } else {
                for (LocalVaadinVersion version : LocalFileManager
                        .getLocalVaadinVersions(false)) {
                    versionMap.put(version.getVersionNumber(), version);
                    versionCombo.add(version.getVersionNumber());
                }
            }
            versionCombo.setText("");

            try {
                // select current version (if any)
                if (project == null) {
                    return;
                }

                // TODO should use getVaadinLibraryVersion()
                IPath vaadinLibrary = ProjectUtil
                        .getVaadinLibraryInProject(project, true);
                if (vaadinLibrary == null) {
                    return;
                }

                // is the Vaadin JAR actually inside the project?
                boolean vaadinInProject = ProjectUtil.isInProject(project,
                        vaadinLibrary);

                String currentVaadinVersionString = VersionUtil
                        .getVaadinVersionFromJar(vaadinLibrary);
                if (currentVaadinVersionString == null) {
                    return;
                }

                // There is a version of the Vaadin jar for the project. It
                // might be in WEB-INF/lib or somewhere else on the classpath.

                // Ensure the version is listed, it might be a custom jar or it
                // might have been removed from the local store for instance
                // when Eclipse was upgraded.

                // TODO should this take dependency versions into account
                // differently?
                LocalVaadinVersion projectVaadinVersion = new LocalVaadinVersion(
                        FileType.VAADIN_RELEASE, currentVaadinVersionString,
                        vaadinLibrary);

                // Always show current version as "6.4.8 (vaadin-*.jar)"
                String comboboxString = currentVaadinVersionString + " ("
                        + projectVaadinVersion.getJarFilename() + ")";

                versionMap.put(comboboxString, projectVaadinVersion);
                // Add the string to the combo box as first
                // ("" becomes second)
                versionCombo.add(comboboxString, 0);
                versionCombo.setText(comboboxString);

                // no longer selectable here
                versionCombo.setEnabled(false);
            } catch (CoreException ce) {
                // ignore if cannot select current version
                ErrorUtil.handleBackgroundException(IStatus.WARNING,
                        "Failed to select the Vaadin version used in the project",
                        ce);
            }
        } catch (CoreException ex) {
            // leave the combo empty and show an error message
            ErrorUtil.displayError("Failed to list downloaded Vaadin versions",
                    ex, getShell());
        }
    }

    /**
     * This method exists only to enable automatic synchronization with a model.
     * The combo box value is the selected version string.
     * 
     * @return Combo
     */
    public Combo getVersionCombo() {
        return versionCombo;
    }

    /**
     * Allow selection of Vaadin 7 versions (dependency management) or other
     * (Vaadin 6) versions.
     * 
     * Note that if false, a Vaadin 7 version already in the project is
     * displayed but cannot be changed and the composite is disabled in that
     * case.
     * 
     * @param useDependencyManagement
     *            true to select Vaadin 7 versions from dependency management
     */
    public void setUseDependencyManagement(boolean useDependencyManagement) {
        this.useDependencyManagement = useDependencyManagement;
        if (versionCombo != null) {
            // refresh everything
            updateView();
        }
    }

    public Composite createContents() {
        addVersionSelectionSection();
        return this;
    }

    public AbstractVaadinVersion getSelectedVersion() {
        AbstractVaadinVersion newVaadinVersion = versionMap
                .get(versionCombo.getText());
        if ("".equals(newVaadinVersion)) {
            newVaadinVersion = null;
        }
        return newVaadinVersion;
    }

    /**
     * Returns the text for the selected item in the version combo box.
     * 
     * @return The string shown in the combo box. Never null.
     */
    public String getSelectedVersionString() {
        return versionCombo.getText();
    }

    public void setProject(IProject project) {
        this.project = project;

        updateView();

        boolean useDependencyManagement = false;
        try {
            IPath vaadinLibrary = ProjectUtil.getVaadinLibraryInProject(project,
                    true);
            useDependencyManagement = (vaadinLibrary == null
                    || !ProjectUtil.isInProject(project, vaadinLibrary));
        } catch (CoreException e) {
            ErrorUtil.handleBackgroundException(
                    "Error trying to check Vaadin version in project", e);
        }
        setUseDependencyManagement(useDependencyManagement);
    }

    public void setNewProject() {
        project = null;

        versionCombo.clearSelection();
    }

    protected void updateView() {
        updateVersionCombo();

        if (null != project) {
            updateNotificationCheckbox.setSelection(
                    PreferenceUtil.get(project).isUpdateNotificationEnabled());
        } else {
            updateNotificationCheckbox.setSelection(true);
        }

        setControlVisible(updateNotificationCheckbox, useDependencyManagement);
        getShell().layout(false);
    }

}