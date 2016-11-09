package com.vaadin.integration.eclipse.properties;

import java.io.IOException;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;

import com.vaadin.integration.eclipse.builder.AddonStylesBuilder;
import com.vaadin.integration.eclipse.builder.AddonStylesImporter;
import com.vaadin.integration.eclipse.builder.WidgetsetBuildManager;
import com.vaadin.integration.eclipse.maven.MavenUtil;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.PreferenceUtil;
import com.vaadin.integration.eclipse.util.ProjectUtil;
import com.vaadin.integration.eclipse.util.WidgetsetUtil;

/**
 * Property page grouping Vaadin Ivy project related project properties.
 * 
 * This page is not used directly as a property page but provides mostly the
 * same API so that {@link VaadinProjectPropertyPage} can forward requests to
 * the appropriate "subpage."
 *
 * Vaadin version selection is here, future subpages may contain more settings.
 */
public class VaadinIvyProjectPropertyPage implements IVaadinPropertyPage {

    private final Image ICON_INFORMATION_SMALL;

    private VaadinVersionComposite vaadinVersionComposite;
    private WidgetsetParametersComposite widgetsetComposite;
    private ThemingParametersComposite themingComposite;

    private IProject project;

    private Composite composite;

    public VaadinIvyProjectPropertyPage() {
        super();
        ICON_INFORMATION_SMALL = new Image(Display.getDefault(), Display
                .getDefault().getSystemImage(SWT.ICON_INFORMATION)
                .getImageData().scaledTo(16, 16));
    }

    public void performDefaults() {
        // revert to the vaadin version currently in the project
        IProject project = getProject();
        vaadinVersionComposite.setProject(project);
        widgetsetComposite.setProject(project);

        if (themingComposite != null) {
            themingComposite.setProject(project);
        }
    }

    public boolean performOk() {
        final IProject project = getProject();
        if (project == null) {
            ErrorUtil.logInfo("Store preferences: not a Vaadin project");
            return true;
        }

        IJavaProject jproject = JavaCore.create(project);

        boolean widgetsetDirty = false;
        Boolean hasWidgetSets = null;

        try {
            widgetsetDirty = updatePreferences(project);

            // if anything changed, mark widgetset as dirty and ask about
            // recompiling it
            if (widgetsetDirty) {
                // will also be saved later, here in case Vaadin version
                // replacement fails
                if (hasWidgetSets == null) {
                    hasWidgetSets = hasWidgetSets(jproject);
                }
                if (hasWidgetSets) {
                    WidgetsetUtil.setWidgetsetDirty(project, true);
                }
            }
        } catch (IOException e) {
            ErrorUtil.displayError(
                    "Failed to save widgetset compilation parameters.", e,
                    getShell());
            ErrorUtil.handleBackgroundException(IStatus.WARNING,
                    "Failed to save widgetset compilation parameters.", e);
            return false;
        }

        try {
            if (themingComposite.isAddonScanningSuspended()
                    || MavenUtil.isMavenProject(project)) {
                AddonStylesBuilder.removeBuilder(project);
            } else {
                AddonStylesBuilder.addBuilder(project);
            }

        } catch (CoreException e) {
            ErrorUtil
            .displayError(
                    "Failed to change Vaadin version in the project. Check that the Vaadin JAR is not in use.",
                    e, getShell());
            ErrorUtil.handleBackgroundException(IStatus.WARNING,
                    "Failed to change Vaadin version in the project", e);
            return false;
        }

        // If anything changed, ask about recompiling the widgetset.
        // Mark the widgetset as dirty only if there is a widgetset in the
        // project.
        if (widgetsetDirty) {
            if (hasWidgetSets == null) {
                hasWidgetSets = hasWidgetSets(jproject);
            }
            if (hasWidgetSets) {
                WidgetsetUtil.setWidgetsetDirty(project, true);
            }
        }

        // this may also be true because of hosted mode launch creation or older
        // changes
        if (WidgetsetUtil.isWidgetsetDirty(project)) {
            WidgetsetBuildManager.runWidgetSetBuildTool(project, false,
                    new NullProgressMonitor());
        }

        return true;
    }

    private boolean updatePreferences(IProject project) throws IOException {
        boolean modifiedValues = false;

        PreferenceUtil preferences = PreferenceUtil.get(project);
        // save widgetset compilation parameters

        boolean suspended = widgetsetComposite.areWidgetsetBuildsSuspended();
        WidgetsetBuildManager.setWidgetsetBuildsSuspended(project, suspended);

        if (AddonStylesImporter.isSupported(project)) {
            boolean wasSuspended = AddonStylesImporter.isSuspended(project);
            suspended = themingComposite.isAddonScanningSuspended();
            AddonStylesImporter.setSuspended(project, suspended);

            if (suspended != wasSuspended) {
                modifiedValues = true;
            }

            if (wasSuspended && !suspended) {
                try {
                    // Trigger addon import scanning if it previously was
                    // suspended
                    // and now again is enabled
                    IFolder themes = ProjectUtil.getThemesFolder(project);
                    if (themes.exists()) {
                        for (IResource theme : themes.members()) {
                            if (theme instanceof IFolder) {
                                IFolder themeFolder = (IFolder) theme;
                                try {
                                    IProgressMonitor monitor = new NullProgressMonitor();
                                    AddonStylesImporter.run(project, monitor,
                                            themeFolder);
                                    themeFolder.refreshLocal(
                                            IResource.DEPTH_INFINITE,
                                            new SubProgressMonitor(monitor, 1));
                                } catch (IOException e) {
                                    ErrorUtil.handleBackgroundException(
                                            IStatus.WARNING,
                                            "Failed to import addon theme folder "
                                                    + themeFolder.getName(), e);
                                }
                            }
                        }
                    }

                } catch (CoreException e) {
                    ErrorUtil.handleBackgroundException(IStatus.WARNING,
                            "Failed to update addons.scss.", e);
                }
            }
        }

        boolean verbose = widgetsetComposite.isVerboseOutput();
        if (preferences.setWidgetsetCompilationVerboseMode(verbose)) {
            modifiedValues = true;
        }

        String style = widgetsetComposite.getCompilationStyle();
        if (preferences.setWidgetsetCompilationStyle(style)) {
            modifiedValues = true;
        }

        String parallelism = widgetsetComposite.getParallelism();
        if (preferences.setWidgetsetCompilationParallelism(parallelism)) {
            modifiedValues = true;
        }

        String extraParams = widgetsetComposite.getExtraParameters();
        if (preferences.setWidgetsetCompilationExtraParameters(extraParams)) {
            modifiedValues = true;
        }

        String extraJvmParams = widgetsetComposite.getExtraJvmParameters();
        if (preferences.setWidgetsetCompilationExtraJvmParameters(extraJvmParams)) {
            modifiedValues = true;
        }

        if (modifiedValues) {
            preferences.persist();
        }

        return modifiedValues;
    }

    private Boolean hasWidgetSets(IJavaProject jproject) {
        try {
            return WidgetsetUtil.hasWidgetSets(jproject,
                    new NullProgressMonitor());
        } catch (CoreException e) {
            ErrorUtil.handleBackgroundException(IStatus.WARNING,
                    "Could not check whether the project "
                            + jproject.getProject().getName()
                            + " has a widgetset", e);
            return false;
        }
    }

    public Control createContents(Composite parent) {
        composite = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout(1, false);
        composite.setLayout(layout);

        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        composite.setLayoutData(data);

        Group group = new Group(composite, SWT.NONE);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        group.setText("Vaadin");
        group.setLayout(new GridLayout(1, false));

        // no dependency management support here
        vaadinVersionComposite = new VaadinVersionComposite(group, SWT.NULL);
        vaadinVersionComposite.createContents();
        vaadinVersionComposite.setUseDependencyManagement(false);

        if (AddonStylesImporter.isSupported(getProject())) {

            group = new Group(composite, SWT.NONE);
            group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            group.setText("Theming");
            group.setLayout(new GridLayout(1, false));

            themingComposite = new ThemingParametersComposite(group, SWT.NULL);
            themingComposite.createContents();
        }

        group = new Group(composite, SWT.NONE);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        group.setText("Widgetsets");
        group.setLayout(new GridLayout(1, false));
        widgetsetComposite = new WidgetsetParametersComposite(group, SWT.NULL);
        widgetsetComposite.createContents();

        performDefaults();

        return composite;
    }
    public void setProject(IProject project) {
        this.project = project;
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