package com.vaadin.integration.eclipse.wizards;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.model.Model;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.ui.internal.MavenImages;
import org.eclipse.m2e.core.ui.internal.Messages;
import org.eclipse.m2e.core.ui.internal.wizards.AbstractMavenProjectWizard;
import org.eclipse.m2e.core.ui.internal.wizards.MavenProjectWizardArchetypeParametersPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;

import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.data.MavenVaadinVersion;
import com.vaadin.integration.eclipse.util.network.MavenVersionManager;

@SuppressWarnings("restriction")
public class Vaadin7MavenProjectWizard extends AbstractMavenProjectWizard
        implements INewWizard {

    /** The wizard page for gathering archetype project information. */
    protected MavenProjectWizardArchetypeParametersPage parametersPage;

    private List<VaadinArchetype> vaadinArchetypes = new ArrayList<VaadinArchetype>();

    private Vaadin7MavenProjectArchetypeSelectionPage vaadinArchetypeSelectionPage;

    private boolean parametersPageInitialized = false;

    /**
     * Default constructor. Sets the title and image of the wizard.
     */
    public Vaadin7MavenProjectWizard() {
        super();
        setWindowTitle("New Vaadin 7 Maven Project");

        // TODO should have own icon
        setDefaultPageImageDescriptor(MavenImages.WIZ_NEW_PROJECT);
        setNeedsProgressMonitor(true);

        // TODO: the list should be populated automatically, possibly from
        // metadata on the server including descriptions from POMs etc.

        // fallback
        String vaadinVersion = "7.6.1";
        try {
            List<MavenVaadinVersion> versions = MavenVersionManager
                    .getAvailableVersions(true);
            if (!versions.isEmpty()) {
                vaadinVersion = versions.get(0).getVersionNumber();
            }
        } catch (CoreException e) {
            // fetching of version list failed - using the default above
            ErrorUtil.handleBackgroundException(e);
        }

        vaadinArchetypes
                .add(new VaadinArchetype("Single-module Application Project",
                        "vaadin-archetype-application", "com.vaadin",
                        vaadinVersion,
                        "A template for a simple Vaadin application.\nPuts everything in one module."));

        vaadinArchetypes
                .add(new VaadinArchetype(
                        "Multi-module Application Project",
                        "vaadin-archetype-application-multimodule",
                        "com.vaadin",
                        vaadinVersion,
                        "A template for a more complex Vaadin project.\nSeparates deployment, UI and widgetset into separate Maven modules."));

        vaadinArchetypes
                .add(new VaadinArchetype(
                        "Add-on Project",
                        "vaadin-archetype-widget",
                        "com.vaadin",
                        vaadinVersion,
                        "A multi-module widget add-on project for creating a re-usable Vaadin component, including a demo application.\nPackages the add-on in a format ready to be deployed to Vaadin Directory."));

        vaadinArchetypes
                .add(new VaadinArchetype(
                        "CRUD Example",
                        "vaadin-archetype-application-example",
                        "com.vaadin",
                        vaadinVersion,
                        "A multi-module example CRUD (create/read/update/delete) application containing a login screen, basic access control examples and more.\nProvides a good example on how you can structure a Vaadin application."));

        // TODO: other archetypes need to be filled in
        // JavaEE CRUD Example
        // Spring CRUD Example
        // Liferay 6 Portlet(Requires separate installation of the Liferay Maven
        // Plugin(?))
    }

    @Override
    public void addPages() {
        /*
         * Vaadin Archetype selection page.
         */
        vaadinArchetypeSelectionPage = new Vaadin7MavenProjectArchetypeSelectionPage(
                vaadinArchetypes);

        /*
         * Archetype parameters page. The only needed page for Vaadin Archetype.
         */
        parametersPage = new MavenProjectWizardArchetypeParametersPage(
                importConfiguration) {

            private static final String DEFAULT_GROUP_ID = "com.example";
            private static final String DEFAULT_ARTIFACT_ID = "myapplication";
            private static final String DEFAULT_SNAPSHOT_VERSION = "1.0-SNAPSHOT";

            @Override
            public void createControl(Composite parent) {
                super.createControl(parent);

                // Input some default values.
                if (groupIdCombo.getText().isEmpty()) {
                    groupIdCombo.setText(DEFAULT_GROUP_ID);
                }

                if (artifactIdCombo.getText().isEmpty()) {
                    artifactIdCombo.setText(DEFAULT_ARTIFACT_ID);
                }

                if (DEFAULT_VERSION.equals(versionCombo.getText())) {
                    versionCombo.setText(DEFAULT_SNAPSHOT_VERSION);
                }
            }

            @Override
            public void setVisible(boolean visible) {
                // This is a workaround for setVisible setting package as
                // customized, even though the user never actually customized
                // it. The customization from wizards point of view was done by
                // overriding this class.

                // If package is not customized before setting visible, we
                // should restore it to not being customized.
                boolean shouldRestore = !packageCustomized;

                super.setVisible(visible);

                String group = groupIdCombo.getText();
                String artifact = artifactIdCombo.getText();

                // Only restore if groupId and artifactId match the defaults.
                if (shouldRestore && (DEFAULT_GROUP_ID.equals(group))
                        && (DEFAULT_ARTIFACT_ID.equals(artifact))) {
                    packageCustomized = false;
                }
            }
        };

        addPage(vaadinArchetypeSelectionPage);
        addPage(parametersPage);
    }

    @Override
    public void setContainer(IWizardContainer wizardContainer) {
        super.setContainer(wizardContainer);

        if (wizardContainer instanceof WizardDialog) {
            ((WizardDialog) wizardContainer)
                    .addPageChangingListener(new IPageChangingListener() {
                        public void handlePageChanging(PageChangingEvent event) {
                            selectArchetype(vaadinArchetypeSelectionPage
                                    .getVaadinArchetype());
                        }
                    });
        }

    }

    /** Returns the model. */
    public Model getModel() {
        return parametersPage.getModel();
    }

    /**
     * To perform the actual project creation, an operation is created and run
     * using this wizard as execution context. That way, messages about the
     * progress of the project creation are displayed inside the wizard.
     * 
     * This method is adapted from MavenProjectWizard.
     */
    @Override
    public boolean performFinish() {
        // this needs to be done to support Finish without ever using Next
        if (!parametersPageInitialized) {
            // only set the archetype, not the "used" flag
            parametersPage.setArchetype(vaadinArchetypeSelectionPage
                    .getVaadinArchetype().getArchetype());
            // this is needed to force loading the parameter default values etc.
            parametersPage.setVisible(true);
        }

        // First of all, we extract all the information from the wizard pages.
        // Note that this should not be done inside the operation we will run
        // since many of the wizard pages' methods can only be invoked from
        // within the SWT event dispatcher thread. However, the operation spawns
        // a new separate thread to perform the actual work, i.e. accessing SWT
        // elements from within that thread would lead to an exception.

        final Model model = getModel();
        final String projectName = importConfiguration.getProjectName(model);
        IStatus nameStatus = importConfiguration.validateProjectName(model);
        if (!nameStatus.isOK()) {
            MessageDialog.openError(getShell(),
                    NLS.bind(Messages.wizardProjectJobFailed, projectName),
                    nameStatus.getMessage());
            return false;
        }

        IWorkspace workspace = ResourcesPlugin.getWorkspace();

        // When using the default workspace location for a project, we have to
        // pass null instead of the actual location.
        final IPath location = null;
        final IWorkspaceRoot root = workspace.getRoot();
        final IProject project = importConfiguration.getProject(root, model);

        boolean pomExists = root.getLocation().append(project.getName())
                .append(IMavenConstants.POM_FILE_NAME).toFile().exists();
        if (pomExists) {
            MessageDialog.openError(getShell(),
                    NLS.bind(Messages.wizardProjectJobFailed, projectName),
                    Messages.wizardProjectErrorPomAlreadyExists);
            return false;
        }

        final Job job;

        final Archetype archetype = vaadinArchetypeSelectionPage
                .getVaadinArchetype().getArchetype();

        final String groupId = model.getGroupId();
        final String artifactId = model.getArtifactId();
        final String version = model.getVersion();
        final String javaPackage = parametersPage.getJavaPackage();
        final Properties properties = parametersPage.getProperties();

        job = new AbstractCreateMavenProjectJob(NLS.bind(
                Messages.wizardProjectJobCreating, archetype.getArtifactId()),
                workingSets) {
            @Override
            protected List<IProject> doCreateMavenProjects(
                    IProgressMonitor monitor) throws CoreException {
                List<IProject> projects = MavenPlugin
                        .getProjectConfigurationManager()
                        .createArchetypeProjects(location,
                                archetype, //
                                groupId, artifactId, version, javaPackage,
                                properties, importConfiguration, monitor);
                return projects;
            }
        };

        job.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                final IStatus result = event.getResult();
                if (!result.isOK()) {
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            MessageDialog.openError(getShell(), //
                                    NLS.bind(Messages.wizardProjectJobFailed,
                                            projectName), result.getMessage());
                        }
                    });
                }
            }
        });

        job.setRule(MavenPlugin.getProjectConfigurationManager().getRule());
        job.schedule();

        return true;
    }

    private void selectArchetype(VaadinArchetype archetype) {
        parametersPage.setArchetype(archetype.getArchetype());
        parametersPage.setUsed(true);
        parametersPage.updatePropertyEditors();
        parametersPageInitialized = true;

        getContainer().updateButtons();
    }

    public static class VaadinArchetype {

        private String title;
        private String description;
        private Archetype archetype;

        public VaadinArchetype(String title, Archetype archetype,
                String description) {
            this.title = title;
            this.archetype = archetype;
            this.description = description;
        }

        public VaadinArchetype(String title, String artifactId, String groupId,
                String version, String description) {
            this.title = title;
            archetype = new Archetype();
            archetype.setArtifactId(artifactId);
            archetype.setGroupId(groupId);
            archetype.setVersion(version);
            this.description = description;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public Archetype getArchetype() {
            return archetype;
        }
    }
}
