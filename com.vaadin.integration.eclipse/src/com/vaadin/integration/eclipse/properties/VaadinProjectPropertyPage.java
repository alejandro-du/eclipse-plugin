package com.vaadin.integration.eclipse.properties;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

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
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.dialogs.PropertyPage;

import com.vaadin.integration.eclipse.builder.AddonStylesBuilder;
import com.vaadin.integration.eclipse.builder.AddonStylesImporter;
import com.vaadin.integration.eclipse.builder.WidgetsetBuildManager;
import com.vaadin.integration.eclipse.properties.VaadinVersionComposite.VersionSelectionChangeListener;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.PreferenceUtil;
import com.vaadin.integration.eclipse.util.ProjectDependencyManager;
import com.vaadin.integration.eclipse.util.ProjectUtil;
import com.vaadin.integration.eclipse.util.WidgetsetUtil;
import com.vaadin.integration.eclipse.util.data.AbstractVaadinVersion;
import com.vaadin.integration.eclipse.util.data.LocalVaadinVersion;
import com.vaadin.integration.eclipse.util.data.MavenVaadinVersion;

/**
 * Property page grouping Vaadin related project properties.
 *
 * Vaadin version selection is here, future subpages may contain more settings.
 */
public class VaadinProjectPropertyPage extends PropertyPage {

    private final Image ICON_INFORMATION_SMALL;

    private VaadinVersionComposite vaadinVersionComposite;
    private WidgetsetParametersComposite widgetsetComposite;
    private ThemingParametersComposite themingComposite;
    private String projectVaadinVersionString;
    private CLabel modifiedLabel;

    public VaadinProjectPropertyPage() {
        super();
        ICON_INFORMATION_SMALL = new Image(Display.getDefault(), Display
                .getDefault().getSystemImage(SWT.ICON_INFORMATION)
                .getImageData().scaledTo(16, 16));
    }

    @Override
    protected void performDefaults() {
        // revert to the vaadin version currently in the project
        try {
            IProject project = getVaadinProject();
            vaadinVersionComposite.setProject(project);
            widgetsetComposite.setProject(project);

            if (themingComposite != null) {
                themingComposite.setProject(project);
            }

            projectVaadinVersionString = vaadinVersionComposite
                    .getSelectedVersionString();
            vaadinVersionSelectValueChange();
        } catch (CoreException ex) {
            ErrorUtil
                    .handleBackgroundException(
                            IStatus.ERROR,
                            "Failed reverting to the Vaadin version currently used in the project",
                            ex);
            vaadinVersionComposite.setProject(null);
        }

    }

    @Override
    public boolean performOk() {
        final IProject project;
        try {
            project = getVaadinProject();
        } catch (CoreException ex) {
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
            if (isVersionChanged()) {
                final AbstractVaadinVersion selectedVaadinVersion = vaadinVersionComposite
                        .getSelectedVersion();

                if (selectedVaadinVersion != null) {
                    ProjectUtil.ensureVaadinFacetAndNature(project);
                }
                boolean versionUpdated = false;
                if (selectedVaadinVersion instanceof LocalVaadinVersion) {
                    versionUpdated = updateProjectVaadinJar(project,
                            (LocalVaadinVersion) selectedVaadinVersion);
                } else if (selectedVaadinVersion instanceof MavenVaadinVersion) {
                    // TODO add support for upgrading a project to Vaadin 7 or
                    // changing Vaadin 7 version in project Ivy configuration
                }
                if (versionUpdated) {
                    widgetsetDirty = true;

                    // Recreate combo box to ensure the changed version is
                    // rendered correctly (if user pushed apply)
                    performDefaults();
                }
            }
            if (themingComposite.isAddonScanningSuspended()) {
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

    /**
     * Updates the project Vaadin jar if needed.
     *
     * @param project
     *            The target project
     * @param selectedVaadinVersion
     *            The version string selected in the version combo box
     * @return true if the version was updated, false otherwise
     * @throws InterruptedException
     * @throws InvocationTargetException
     * @throws CoreException
     */
    private boolean updateProjectVaadinJar(final IProject project,
            final LocalVaadinVersion selectedVaadinVersion)
            throws CoreException {

        // Do the actual update
        IRunnableWithProgress op = new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor)
                    throws InvocationTargetException {
                try {
                    ProjectDependencyManager.updateVaadinLibraries(project,
                            selectedVaadinVersion, monitor);
                } catch (CoreException e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            }
        };
        try {
            new ProgressMonitorDialog(getShell()).run(true, true, op);
        } catch (InvocationTargetException e) {
            Throwable realException = e.getTargetException();
            throw ErrorUtil.newCoreException(
                    "Failed to updated Vaadin library in project",
                    realException);
        } catch (InterruptedException e) {
            throw ErrorUtil.newCoreException(
                    "Failed to updated Vaadin library in project", e);
        }

        return true;
    }

    private boolean updatePreferences(IProject project) throws IOException {
        boolean modifiedValues = false;

        PreferenceUtil preferences = PreferenceUtil.get(project);
        // save widgetset compilation parameters

        boolean useLatestNightly = vaadinVersionComposite.isUseLatestNightly();
        if (preferences.setUsingLatestNightly(useLatestNightly)) {
            modifiedValues = true;
        }

        boolean suspended = widgetsetComposite.areWidgetsetBuildsSuspended();
        WidgetsetBuildManager.setWidgetsetBuildsSuspended(project, suspended);

        if (AddonStylesImporter.isSupported(project)) {
            boolean wasSuspended = AddonStylesImporter.isSuspended(project);
            suspended = themingComposite.isAddonScanningSuspended();
            AddonStylesImporter.setSuspended(project, suspended);
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

    /**
     * @see PreferencePage#createContents(Composite)
     */
    @Override
    protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.NULL);
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
        vaadinVersionComposite
                .setVersionSelectionListener(new VersionSelectionChangeListener() {

                    public void versionChanged() {
                        vaadinVersionSelectValueChange();
                    }
                });

        modifiedLabel = new CLabel(group, SWT.NONE);
        modifiedLabel.setImage(ICON_INFORMATION_SMALL);
        modifiedLabel.setText("");
        modifiedLabel.setVisible(false);
        modifiedLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        try {
            if (AddonStylesImporter.isSupported(getVaadinProject())) {

                group = new Group(composite, SWT.NONE);
                group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                group.setText("Theming");
                group.setLayout(new GridLayout(1, false));

                themingComposite = new ThemingParametersComposite(group,
                        SWT.NULL);
                themingComposite.createContents();
            }
        } catch (CoreException e) {
            ErrorUtil.handleBackgroundException(IStatus.WARNING,
                    "Failed to create theming option group", e);
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

    protected void vaadinVersionSelectValueChange() {
        if (isVersionChanged()) {
            modifiedLabel.setVisible(true);
            String willHappen = "Vaadin jar in the project will be ";
            if (vaadinVersionComposite.getSelectedVersionString().equals("")) {
                willHappen += "removed";
            } else {
                willHappen += "updated";
            }
            modifiedLabel.setText(willHappen);
        } else {
            modifiedLabel.setVisible(false);
        }

    }

    public boolean isVersionChanged() {
        String selectedVersionString = vaadinVersionComposite
                .getSelectedVersionString();
        if (projectVaadinVersionString == null) {
            return !selectedVersionString.equals("");
        }

        return !projectVaadinVersionString.equals(selectedVersionString);
    }

    private IProject getVaadinProject() throws CoreException {
        IProject project;
        if (getElement() instanceof IJavaProject) {
            project = ((IJavaProject) getElement()).getProject();
        } else if (getElement() instanceof IProject) {
            project = (IProject) getElement();
        } else {
            throw ErrorUtil.newCoreException("Not a Vaadin project", null);
        }
        return project;
    }

    @Override
    public void dispose() {
        super.dispose();
        ICON_INFORMATION_SMALL.dispose();
    }
}