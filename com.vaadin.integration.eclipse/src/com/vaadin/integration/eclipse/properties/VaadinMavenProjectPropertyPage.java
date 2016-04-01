package com.vaadin.integration.eclipse.properties;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.m2e.core.ui.internal.UpdateMavenProjectJob;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.preferences.PreferenceConstants;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.PreferenceUtil;

/**
 * Property page grouping Vaadin Maven project related project properties.
 * 
 * This page is not used directly as a property page but provides mostly the
 * same API so that {@link VaadinProjectPropertyPage} can forward requests to
 * the appropriate "subpage."
 */
public class VaadinMavenProjectPropertyPage implements IVaadinPropertyPage {

    private final Image ICON_INFORMATION_SMALL;

    private IProject project;

    private Composite composite;

    private StaticVaadinVersionComposite versionComposite;
    private Button overrideButton;
    private Button autoCompileButton;

    public VaadinMavenProjectPropertyPage() {
        super();
        ICON_INFORMATION_SMALL = new Image(Display.getDefault(), Display
                .getDefault().getSystemImage(SWT.ICON_INFORMATION)
                .getImageData().scaledTo(16, 16));
    }

    public void performDefaults() {
        initializeAutoCompileState(project);
    }

    public boolean performOk() {
        final IProject project = getProject();
        if (project == null) {
            ErrorUtil.logInfo("Store preferences: not a Vaadin project");
            return true;
        }

        try {
            updatePreferences(project);
        } catch (IOException e) {
            ErrorUtil.handleBackgroundException(
                    "Could not save project preferences for " + project, e);
        }

        return true;
    }

    private boolean updatePreferences(IProject project) throws IOException {
        boolean modifiedValues = false;

        PreferenceUtil preferences = PreferenceUtil.get(project);

        Boolean newAutoCompilePref;
        Boolean oldAutoCompilePref = PreferenceUtil.get(project)
                .isMavenAutoCompileWidgetset();
        if (overrideButton.getSelection()) {
            newAutoCompilePref = autoCompileButton.getSelection();
        } else {
            newAutoCompilePref = null;
        }
        if (newAutoCompilePref != oldAutoCompilePref) {
            modifiedValues = true;
            PreferenceUtil.get(project).setMavenAutoCompileWidgetset(
                    newAutoCompilePref);

            // if necessary, trigger a build to auto-compile the widgetset
            boolean oldEffectiveAutoCompile = Boolean.TRUE
                    .equals(oldAutoCompilePref)
                    || (oldAutoCompilePref == null && isWorkspaceAutoCompileWidgetset());
            ;
            boolean newEffectiveAutoCompile = Boolean.TRUE
                    .equals(newAutoCompilePref)
                    || (newAutoCompilePref == null && isWorkspaceAutoCompileWidgetset());
            if (!oldEffectiveAutoCompile && newEffectiveAutoCompile) {
                // trigger Maven project update and build
                new UpdateMavenProjectJob(new IProject[] { project }, true,
                        false, true, false, true).schedule();
            }
        }

        if (modifiedValues) {
            preferences.persist();
        }


        return modifiedValues;
    }

    /**
     * @see PreferencePage#createContents(Composite)
     */
    public Control createContents(Composite parent) {
        composite = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout(1, false);
        composite.setLayout(layout);

        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        composite.setLayoutData(data);

        versionComposite = new StaticVaadinVersionComposite(composite, SWT.NONE);

        overrideButton = new Button(composite, SWT.CHECK);
        overrideButton.setText("Enable project specific settings");

        Label horizontalLine = new Label(composite, SWT.SEPARATOR
                | SWT.HORIZONTAL);
        horizontalLine.setLayoutData(new GridData(GridData.FILL, GridData.FILL,
                true, false, 1, 1));
        horizontalLine.setFont(composite.getFont());

        autoCompileButton = new Button(composite, SWT.CHECK);
        autoCompileButton.setText("Enable automatic widgetset compilation");

        overrideButton.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                autoCompileButton.setEnabled(overrideButton.getSelection());
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                autoCompileButton.setEnabled(overrideButton.getSelection());
            }
        });

        setProject(project);

        return composite;
    }

    public void setProject(IProject project) {
        this.project = project;

        // if initialized
        if (versionComposite != null) {
            versionComposite.setProject(project);

            initializeAutoCompileState(project);
        }
    }

    private void initializeAutoCompileState(IProject project) {
        Boolean autoCompilePref = PreferenceUtil.get(project)
                .isMavenAutoCompileWidgetset();
        if (autoCompilePref == null) {
            overrideButton.setSelection(false);
            boolean globalSetting = isWorkspaceAutoCompileWidgetset();
            autoCompileButton.setSelection(globalSetting);
            autoCompileButton.setEnabled(false);
        } else {
            overrideButton.setSelection(true);
            autoCompileButton.setSelection(autoCompilePref);
            autoCompileButton.setEnabled(true);
        }
    }

    private boolean isWorkspaceAutoCompileWidgetset() {
        return VaadinPlugin
                .getInstance()
                .getPreferenceStore()
                .getBoolean(
                        PreferenceConstants.MAVEN_WIDGETSET_AUTOMATIC_BUILD_ENABLED);
    }

    public IProject getProject() {
        return project;
    }

    private Shell getShell() {
        return composite.getShell();
    }

    public void dispose() {
        ICON_INFORMATION_SMALL.dispose();
    }

    public Control getControl() {
        return composite;
    }
}