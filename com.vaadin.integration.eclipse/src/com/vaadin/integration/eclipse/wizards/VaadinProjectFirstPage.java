package com.vaadin.integration.eclipse.wizards;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.jst.j2ee.web.project.facet.IWebFacetInstallDataModelProperties;
import org.eclipse.jst.servlet.ui.project.facet.WebFacetInstallPage;
import org.eclipse.jst.servlet.ui.project.facet.WebProjectFirstPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.wst.common.componentcore.datamodel.properties.IFacetDataModelProperties;
import org.eclipse.wst.common.frameworks.datamodel.DataModelEvent;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.frameworks.datamodel.IDataModelListener;
import org.eclipse.wst.common.frameworks.internal.datamodel.ui.DataModelSynchHelper;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;

import com.vaadin.integration.eclipse.IVaadinFacetInstallDataModelProperties;
import com.vaadin.integration.eclipse.VaadinFacetUtils;
import com.vaadin.integration.eclipse.properties.VaadinVersionComposite;

/**
 * The first (main) page of the Vaadin top-level project creation wizard.
 * 
 * This replaces WebProjectFirstPage when creating a Vaadin project through its
 * own wizard, and collects the key configuration items on the first page so
 * that the user can typically just click Finish after the first page.
 * 
 * Some settings are omitted when creating a project through the Vaadin wizard.
 */
@SuppressWarnings("restriction")
public class VaadinProjectFirstPage extends WebProjectFirstPage implements
        IVaadinFacetInstallDataModelProperties {

    private Combo projectTypeCombo;

    public VaadinProjectFirstPage(IDataModel model, String pageName) {
        super(model, pageName);

        setTitle("Vaadin Project");
        setDescription("Create a Vaadin Dynamic Web project.");
        // setImageDescriptor(J2EEUIPlugin.getDefault().getImageDescriptor(
        // J2EEUIPluginIcons.WEB_PROJECT_WIZARD_BANNER));
    }

    @Override
    protected String getModuleFacetID() {
        return VaadinFacetUtils.VAADIN_FACET_ID;
    }

    @Override
    protected Composite createTopLevelComposite(Composite parent) {
        Composite top = new Composite(parent, SWT.NONE);
        top.setLayout(new GridLayout());
        top.setLayoutData(new GridData(GridData.FILL_BOTH));
        createProjectGroup(top);
        createServerTargetComposite(top);
        // createPrimaryFacetComposite(top);
        createPresetPanel(top);

        // Vaadin key settings on the first page
        createVaadinComposite(top);

        // createWorkingSetGroupPanel(top, new String[] { RESOURCE_WORKING_SET,
        // JAVA_WORKING_SET });

        return top;
    }

    @Override
    public IProjectFacetVersion getPrimaryFacetVersion() {
        // overriding this method is needed when primary facet composite is not
        // added

        // get from data model if set there, use 7.0 as default
        IProjectFacetVersion facetVersion = (IProjectFacetVersion) model
                .getProperty(IFacetDataModelProperties.FACET_VERSION);

        // TODO this first returns V7 for a New V6 project wizard???
        return (facetVersion != null) ? facetVersion
                : VaadinFacetUtils.VAADIN_70;
    }

    // this is partly duplicated in VaadinCoreFacetInstallPage
    protected Composite createVaadinComposite(final Composite parent) {
        final Group group = new Group(parent, SWT.NONE);
        group.setLayoutData(gdhfill());
        group.setLayout(new GridLayout(1, false));
        group.setText("Vaadin"); //$NON-NLS-1$

        // synchronize fields with the Vaadin facet data model instead of the
        // project data model
        FacetDataModelMap map = (FacetDataModelMap) model
                .getProperty(FACET_DM_MAP);
        IDataModel vaadinFacetDataModel = map
                .getFacetDataModel(VaadinFacetUtils.VAADIN_FACET_ID);
        DataModelSynchHelper vaadinFacetSynchHelper = new DataModelSynchHelper(
                vaadinFacetDataModel);

        boolean vaadin7 = VaadinFacetUtils.VAADIN_70
                .equals(getPrimaryFacetVersion());

        Label label = new Label(group, SWT.NONE);
        label.setLayoutData(gdhfill());
        label.setText("Deployment configuration:");

        projectTypeCombo = new Combo(group, SWT.READ_ONLY);
        projectTypeCombo.setLayoutData(gdhfill());
        // contents initialized depending on facet version by
        // VaadinFacetInstallDataModelProvider.getValidPropertyDescriptors()
        vaadinFacetSynchHelper.synchCombo(projectTypeCombo,
                VAADIN_PROJECT_TYPE, new Control[] { label });

        // Vaadin version selection
        final VaadinVersionComposite versionComposite = new VaadinVersionComposite(
                group, SWT.NULL);
        versionComposite.createContents();
        versionComposite.setUseDependencyManagement(vaadin7);

        versionComposite.setNewProject();

        // synch version string to model
        synchHelper.synchCombo(versionComposite.getVersionCombo(),
                VAADIN_VERSION, new Control[] {});

        // Add a check box for choosing whether to create a TB test.
        GridData gd = new GridData(GridData.FILL, GridData.BEGINNING, true,
                false);
        gd.horizontalSpan = 3;
        final Button b = new Button(versionComposite, SWT.CHECK);
        b.setText("Create TestBench test");
        b.setLayoutData(gd);
        synchHelper.synchCheckbox(b, CREATE_TB_TEST, new Control[] {});

        // update Vaadin 6/7
        vaadinFacetDataModel.addListener(new IDataModelListener() {
            public void propertyChanged(DataModelEvent event) {
                if (IFacetDataModelProperties.FACET_VERSION.equals(event
                        .getPropertyName())) {
                    boolean vaadin7 = VaadinFacetUtils.VAADIN_70
                            .equals(getPrimaryFacetVersion());
                    versionComposite.setUseDependencyManagement(vaadin7);
                    GridData data = (GridData) b.getLayoutData();
                    data.exclude = !vaadin7;
                    b.setVisible(vaadin7);
                }
            }
        });

        return group;
    }

    @Override
    protected String[] getValidationPropertyNames() {
        String[] superProperties = super.getValidationPropertyNames();
        ArrayList<String> arrayList = new ArrayList<String>();
        arrayList.addAll(Arrays.asList(superProperties));
        // validation of these relies on nested models in the project level
        // model - see VaadinProjectCreationDataModelProvider
        arrayList.add(APPLICATION_NAME);
        arrayList.add(APPLICATION_PACKAGE);
        arrayList.add(APPLICATION_CLASS);
        arrayList.add(APPLICATION_THEME);
        arrayList.add(VAADIN_PROJECT_TYPE);
        arrayList.add(IWebFacetInstallDataModelProperties.CONTEXT_ROOT);
        // validating this leads to strange behavior for Finish button
        // enabling/disabling when changing the value
        // arrayList.add(IWebFacetInstallDataModelProperties.CONFIG_FOLDER);
        return arrayList.toArray(new String[0]);
    }

    @SuppressWarnings("unused")
    private static final class WebFacetResources extends NLS {
        public static String pageTitle;
        // public static String pageDescription;
        public static String contextRootLabel;
        public static String contextRootLabelInvalid;
        public static String contentDirLabel;
        public static String contentDirLabelInvalid;

        static {
            initializeMessages(WebFacetInstallPage.class.getName(),
                    WebFacetResources.class);
        }
    }

}
