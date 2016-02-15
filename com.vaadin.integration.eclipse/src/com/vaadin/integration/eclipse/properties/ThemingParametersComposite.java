package com.vaadin.integration.eclipse.properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.vaadin.integration.eclipse.builder.AddonStylesImporter;
import com.vaadin.integration.eclipse.maven.MavenUtil;

public class ThemingParametersComposite extends Composite {

    private Button suspendAddonThemeScanning;

    private IProject project;

    public ThemingParametersComposite(Composite parent, int style) {
        super(parent, style);
    }

    public Composite createContents() {
        setLayout(new GridLayout(1, false));
        setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

        Composite options = new Composite(this, SWT.NULL);
        options.setLayout(new GridLayout(2, false));
        options.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

        suspendAddonThemeScanning = new Button(options, SWT.CHECK);
        suspendAddonThemeScanning
                .setText("Suspend automatic addon theme scanning");

        return this;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (suspendAddonThemeScanning != null) {
            suspendAddonThemeScanning.setEnabled(enabled
                    && !MavenUtil.isMavenProject(project));
        }
    }

    public void setProject(IProject project) {
        this.project = project;
        if (MavenUtil.isMavenProject(project)) {
            suspendAddonThemeScanning.setEnabled(false);
        } else {
            boolean suspendend = AddonStylesImporter.isSuspended(project);
            suspendAddonThemeScanning.setSelection(suspendend);
        }
    }

    public boolean isAddonScanningSuspended() {
        return suspendAddonThemeScanning.getSelection();
    }
}
