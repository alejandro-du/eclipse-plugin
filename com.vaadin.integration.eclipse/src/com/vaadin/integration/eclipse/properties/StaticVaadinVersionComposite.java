package com.vaadin.integration.eclipse.properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.vaadin.integration.eclipse.util.ProjectUtil;

/**
 * Project property page for selecting an Vaadin version and updating the JAR in
 * the project.
 */
public class StaticVaadinVersionComposite extends Composite {

    private IProject project = null;
    private Button updateNotificationCheckbox;
    private Label versionLabel;

    public StaticVaadinVersionComposite(Composite parent, int style) {
        super(parent, style);

        setLayout(new GridLayout(1, false));
        GridData data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        setLayoutData(data);

        versionLabel = new Label(this, SWT.NONE);
        GridData labelGridData = new GridData(GridData.FILL,
                GridData.BEGINNING, true, false);
        versionLabel.setLayoutData(labelGridData);

        getShell().layout(false);
    }

    private void updateVersionLabel() {
        try {
            String vaadinVersion = ProjectUtil.getVaadinLibraryVersion(project,
                    true);
            if (vaadinVersion != null) {
                versionLabel.setText("Vaadin version: " + vaadinVersion);
                return;
            }
        } catch (CoreException ce) {
            // ignore if cannot select current version
        }
        // fallback for all error cases
        versionLabel.setText("");
    }

    public void setProject(IProject project) {
        this.project = project;

        updateVersionLabel();

        getShell().layout(false);
    }

}