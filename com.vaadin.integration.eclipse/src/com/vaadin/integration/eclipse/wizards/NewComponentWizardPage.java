package com.vaadin.integration.eclipse.wizards;

import java.util.Arrays;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.templates.TEMPLATES;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.ProjectUtil;
import com.vaadin.integration.eclipse.util.VaadinPluginUtil;
import com.vaadin.integration.eclipse.util.WidgetsetUtil;

// TODO rename as NewWidgetWizardPage?
@SuppressWarnings("restriction")
public class NewComponentWizardPage extends AbstractVaadinNewTypeWizardPage {

    private Combo extWidgetSetNameText;

    private Label extWidgetSetNameLabel;

    private String widgetsetName;

    // private ICompilationUnit createdClientSideClass;

    private double projectVersion; // maj.min

    private Combo templateCombo;
    private Label templateDescriptionLabel;

    private TEMPLATES currentTemplate;

    private IStructuredSelection selection;

    /**
     * Constructor for Component wizard page.
     *
     * @param pageName
     */
    public NewComponentWizardPage(IProject project,
            IStructuredSelection selection) {
        super("componentwizard", project);
        setTitle("New Component wizard");
        setDescription("This wizard creates a new Vaadin widget.");

        setTypeName("MyComponent", true);
        setSuperClass(VaadinPlugin.VAADIN_PACKAGE_PREFIX
                + "ui.AbstractComponent", true);

        this.selection = selection;
    }

    @Override
    public void setPackageFragmentRoot(IPackageFragmentRoot root,
            boolean canBeModified) {
        // TODO Auto-generated method stub
        super.setPackageFragmentRoot(root, canBeModified);
    }

    // this is called by setPackageFragmentRoot
    @Override
    protected void setProjectInternal(IProject project) {
        super.setProjectInternal(project);

        // clear the widgetset combo in any case
        if (extWidgetSetNameText != null) {
            extWidgetSetNameText.removeAll();
        }

        // show the page even when there is no project - eclipse guidelines
        if (project == null || !ProjectUtil.isVaadin7(project)) {
            return;
        }

        try {
            IJavaElement elem = getInitialJavaElement(selection);
            while (elem != null
                    && elem.getElementType() != IJavaElement.PACKAGE_FRAGMENT) {
                elem = elem.getParent();
            }
            if (elem != null) {
                // must be package
                setPackageFragment((IPackageFragment) elem, true);
            } else {
                // Detect a package where an Application or UI lies in as a
                // default package
                IType projectApplicationOrUI = null;
                IType[] prospects = VaadinPluginUtil.getApplicationClasses(
                        project, null);
                if (prospects.length == 0) {
                    prospects = VaadinPluginUtil.getUiClasses(project, null);
                }

                if (prospects.length > 0) {
                    projectApplicationOrUI = prospects[0];
                    IPackageFragment packageFragment = projectApplicationOrUI
                            .getPackageFragment();
                    setPackageFragment(packageFragment, true);
                } else {
                    // if there is no application, reset the fields of the page
                    setPackageFragment(null, true);
                    // but continue and possibly set up the rest later
                }
            }

            setTypeName("MyComponent", true);

            projectVersion = ProjectUtil.getVaadinVersion(project);
            if (projectVersion >= 6.2) {
                if (isControlCreated()) {
                    extWidgetSetNameText.setVisible(false);
                    extWidgetSetNameLabel.setVisible(false);
                }

            } else {
                if (isControlCreated()) {
                    extWidgetSetNameText.setVisible(true);
                    extWidgetSetNameLabel.setVisible(true);
                }
                // Detect widgetsets in this project and update the combo
                IType[] wsSubtypes = WidgetsetUtil.getWidgetSetClasses(project,
                        null);

                if (extWidgetSetNameText != null) {
                    for (IType ws : wsSubtypes) {
                        if (project.equals(ws.getResource().getProject())) {
                            extWidgetSetNameText
                            .add(ws.getFullyQualifiedName());
                        }
                    }

                    // check that there is a widgetset before selecting one
                    if (extWidgetSetNameText.getItemCount() > 0) {
                        extWidgetSetNameText.setText(extWidgetSetNameText
                                .getItem(0));
                    }
                }
            }

            updateTemplateCombo();

        } catch (CoreException e1) {
            ErrorUtil
            .handleBackgroundException(
                    IStatus.WARNING,
                    "Failed to select the project in the New Widget wizard",
                    e1);
        }
    }

    private void updateTemplateCombo() {
        if (templateCombo != null) {
            templateCombo.removeAll();
            for (TEMPLATES template : TEMPLATES.values()) {
                if (template.isSuitableFor(projectVersion)) {
                    templateCombo.add(template.getTitle());
                    templateCombo.setData(template.getTitle(), template);
                }
            }
            templateCombo.addSelectionListener(new SelectionListener() {

                public void widgetDefaultSelected(SelectionEvent e) {
                    // NOP
                }

                public void widgetSelected(SelectionEvent e) {
                    selectTemplate((TEMPLATES) templateCombo
                            .getData(templateCombo.getText()));
                }
            });

            // avoid NPE in the case of an unknown/unavailable Vaadin version
            if (templateCombo.getItemCount() > 0) {
                templateCombo.select(0);
                selectTemplate((TEMPLATES) templateCombo.getData(templateCombo
                        .getText()));
            }

            // Ensure combobox width is updated if the contents change
            templateCombo.getParent().layout();
        }
    }

    /**
     * @see IDialogPage#createControl(Composite)
     */
    public void createControl(Composite parent) {

        initializeDialogUnits(parent);

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setFont(parent.getFont());

        int nColumns = 4;

        GridLayout layout = new GridLayout();
        layout.numColumns = nColumns;
        composite.setLayout(layout);

        // pick & choose the wanted UI components

        createContainerControls(composite, nColumns);
        createPackageControls(composite, nColumns);

        // createEnclosingTypeControls(composite, nColumns);

        // createSeparator(composite, nColumns);

        createTypeNameControls(composite, nColumns);
        // createModifieCrControls(composite, nColumns);

        createSuperClassControls(composite, nColumns);

        // createSuperInterfacesControls(composite, nColumns);

        // createCommentControls(composite, nColumns);
        // enableCommentControl(true);

        createClientSideControls(composite, nColumns);

        setControl(composite);

        Dialog.applyDialogFont(composite);

    }

    private void createClientSideControls(Composite composite, int columns) {

        GridData gd = new GridData(GridData.FILL_HORIZONTAL);

        Label l = new Label(composite, SWT.NULL);
        l.setText("Template:");
        templateCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);

        DialogField.createEmptySpace(composite, columns - 2);

        // TODO show template description somewhere - framed (multiline) label?
        templateDescriptionLabel = new Label(composite, SWT.NULL);
        GridData wideGd = new GridData(SWT.FILL, SWT.BEGINNING, true, false,
                columns, 1);
        templateDescriptionLabel.setLayoutData(wideGd);

        extWidgetSetNameLabel = new Label(composite, SWT.NULL);
        extWidgetSetNameLabel.setText("To widgetset:");

        extWidgetSetNameText = new Combo(composite, SWT.DROP_DOWN
                | SWT.READ_ONLY);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        extWidgetSetNameText.setLayoutData(gd);
        extWidgetSetNameText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                widgetsetName = extWidgetSetNameText.getText();

            }
        });

        updateTemplateCombo();
    }

    protected void selectTemplate(TEMPLATES template) {
        currentTemplate = template;

        templateDescriptionLabel.setText(template.getDescription());

        boolean buildClientSideStub = template.hasClientTemplates();
        // if less than 6.2, show these
        extWidgetSetNameText.setVisible(template.isSuitableFor(6)
                && buildClientSideStub);
        extWidgetSetNameLabel.setVisible(template.isSuitableFor(6)
                && buildClientSideStub);

        String prefix = VaadinPlugin.VAADIN_PACKAGE_PREFIX;
        if (template.hasClientTemplates()) {
            setSuperClass(prefix + "ui.AbstractComponent", true);
        } else {
            setSuperClass(prefix + "ui.CustomComponent", true);
        }
    }

    @Override
    protected void createTypeMembers(IType type, ImportsManager imports,
            IProgressMonitor monitor) throws CoreException {

        String prefix = VaadinPlugin.VAADIN_PACKAGE_PREFIX;

        // server-side CustomComponent
        if (!currentTemplate.hasClientTemplates()
                && getSuperClass().endsWith("ui.CustomComponent")) {
            // CustomComponent must set composition root
            imports.addImport(prefix + "ui.Label");

            type.createMethod(
                    "\n\tpublic "
                            + type.getElementName()
                            + "() {\n"
                            + "\t\tsetCompositionRoot(new Label(\"Custom component\"));\n"
                            + "\t}\n", null, false, monitor);
        }

        // pre-6.2 getTag()
        if (currentTemplate.isSuitableFor(6)
                && currentTemplate.hasClientTemplates()) {
            type.createMethod(
                    "@Override\n\tpublic String getTag(){\n\t\treturn \""
                            + type.getElementName().toLowerCase() + "\" ;\n}\n",
                            null, false, monitor);
        }

        if (currentTemplate.hasClientTemplates()) {
            for (Class templateClass : currentTemplate.getClientTemplates()) {
                // template prefix for server-side methods
                // String templateBase = "component/" + templateName;

                // try {
                if (currentTemplate.hasClientTemplates()) {
                    imports.addImport("java.util.Map");
                    imports.addImport(prefix + "terminal.PaintException");
                    imports.addImport(prefix + "terminal.PaintTarget");
                }

            }
        }
    }

    public String getWidgetSetName() {
        return widgetsetName;
    }

    public TEMPLATES getTemplate() {
        return currentTemplate;
    }

    @Override
    protected IStatus[] getStatus() {
        IStatus[] status = super.getStatus();
        if (currentTemplate != null && currentTemplate.hasClientTemplates()
                && currentTemplate.isSuitableFor(6)
                && extWidgetSetNameText.getItemCount() == 0) {
            // no widgetset exists
            IStatus[] newStatus = new IStatus[status.length + 1];
            System.arraycopy(status, 0, newStatus, 0, status.length);
            newStatus[newStatus.length - 1] = new Status(IStatus.ERROR,
                    VaadinPlugin.PLUGIN_ID, "No widgetset in project.");
            status = newStatus;
        }

        // see if the selected superclass implements Component
        String sc = getSuperClass();
        boolean found = false;
        IJavaProject proj = getJavaProject();
        if (proj == null) {
            // no project
            // TODO this really screws everything, find a better way...
            return status;
        }
        while (!found && sc != null && !"".equals(sc)) {
            IType type = null;
            try {
                type = getJavaProject().findType(sc);
            } catch (JavaModelException e) {
                // you're making up types, are you not?
                break;
            }
            if (type == null || !type.exists()) {
                // you're making up types, are you not?
                break;
            }
            try {
                String[] interfaces = type.getSuperInterfaceNames();
                found = interfaces != null
                        && Arrays.binarySearch(interfaces,
                                "com.vaadin.ui.Component") >= 0;
            } catch (JavaModelException e) {
                // whatever, let's try upwards anyway
            }
            try {
                sc = type.getSuperclassName();
            } catch (JavaModelException e) {
                // no good
                break;
            }
        }
        if (!found) {
            IStatus[] newStatus = new IStatus[status.length + 1];
            System.arraycopy(status, 0, newStatus, 0, status.length);
            newStatus[newStatus.length - 1] = new Status(IStatus.ERROR,
                    VaadinPlugin.PLUGIN_ID,
                    "Superclass must be a com.vaadin.ui.Component");
            status = newStatus;
        }

        return status;
    }
}