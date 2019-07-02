package com.vaadin.integration.eclipse.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.vaadin.integration.eclipse.builder.WidgetsetBuildManager;
import com.vaadin.integration.eclipse.templates.TEMPLATES;
import com.vaadin.integration.eclipse.templates.Template;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.ProjectDependencyManager;
import com.vaadin.integration.eclipse.util.ProjectUtil;
import com.vaadin.integration.eclipse.util.WidgetsetUtil;

public class NewComponentWizard extends Wizard implements INewWizard {
    private NewComponentWizardPage page;
    private IStructuredSelection selection;

    // created files, will be opened in the IDE
    private List<IFile> createdFiles = new LinkedList<IFile>();

    /**
     * Constructor for new Component wizard.
     */
    public NewComponentWizard() {
        super();
        setWindowTitle("New Vaadin widget");
        setNeedsProgressMonitor(true);
    }

    /**
     * Adding the page to the wizard.
     */
    @Override
    public void addPages() {
        IProject project = ProjectUtil.getProject(selection);
        page = new NewComponentWizardPage(project, selection);
        addPage(page);
    }

    /**
     * This method is called when 'Finish' button is pressed in the wizard. We
     * will create an operation and run it using wizard as execution context.
     */
    @Override
    public boolean performFinish() {
        IRunnableWithProgress op = new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor)
                    throws InvocationTargetException {
                try {
                    doFinish(monitor);
                } catch (CoreException e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            }
        };
        try {
            getContainer().run(true, false, op);
        } catch (InterruptedException e) {
            return false;
        } catch (InvocationTargetException e) {
            Throwable realException = e.getTargetException();
            MessageDialog.openError(getShell(), "Error",
                    realException.getMessage());
            return false;
        }
        return true;
    }

    /**
     * The worker method. It will find the container, create the file if missing
     * or just replace its contents, and open the editor on the newly created
     * file.
     */
    private void doFinish(IProgressMonitor monitor) throws CoreException {
        // create a sample file
        monitor.beginTask("Creating widget", 10);

        TEMPLATES template = page.getTemplate();
        if (template.hasClientTemplates()) {
            ProjectDependencyManager.ensureGWTLibraries(page.getProject(),
                    new SubProgressMonitor(monitor, 5));

            buildClientSideClass(template, monitor);
            monitor.worked(1);

        }

        openFiles();
        monitor.worked(1);

        // Trigger widgetset compilation dialog
        IProject project = page.getProject();
        if (WidgetsetUtil.isWidgetsetDirty(project)) {
            WidgetsetBuildManager.runWidgetSetBuildTool(project, false,
                    new NullProgressMonitor());
        }

    }

    private void buildClientSideClass(TEMPLATES template,
            IProgressMonitor monitor) throws CoreException {
        // we know that a client side widget should be built
        String widgetSetName;

        // 6.2+
        // Create the widgetset if it did not exist. This way, the user can
        // immediately move the widgetset package instead of needing to
        // compile widgetset first; the package is determined based on the
        // new widget's package
        widgetSetName = WidgetsetUtil.getWidgetSet(page.getJavaProject(), true,
                page.getPackageFragmentRoot(), page.getPackageText(), monitor);

        IJavaProject javaProject = page.getJavaProject();
        String typeName = page.getTypeName();
        try {
            IPackageFragmentRoot packageFragmentRoot = page
                    .getPackageFragmentRoot();
            final String packageName;
            // 6.2+ (remove typename)
            packageName = widgetSetName.replaceAll("\\.[^\\.]+$", "");

            // Server-side component location, e.g com.example.MyComponent
            String componentPackage = page.getPackageFragment()
                    .getElementName();

            // Server-side component extends, e.g
            // com.vaadin.ui.AbstractComponent
            String componentExtends = page.getSuperClass();

            // Figure what State should extend, e.g
            // com.vaadin.client.ComponentState
            String stateExtends = null;
            if (template.hasState()) {
                IType extType = javaProject.findType(componentExtends);
                IMethod getStateMethod = extType.getMethod("getState", null);
                while (!getStateMethod.exists()) {
                    String parentName = extType.getSuperclassName();
                    if (parentName == null) {
                        break;
                    }
                    extType = javaProject.findType(parentName);
                    getStateMethod = extType.getMethod("getState", null);
                }
                if (getStateMethod.exists()) {
                    stateExtends = getStateMethod.getReturnType();
                    stateExtends = Signature.toString(stateExtends);
                } else {
                    stateExtends = "com.vaadin.terminal.gwt.client.ComponentState";
                }
            }

            // run all templates
            for (Class<Template> c : template.getClientTemplates()) {
                Template t = c.newInstance();
                String src = t.generate(typeName, componentPackage,
                        componentExtends, stateExtends, packageName, template);

                IPackageFragment targetPackage = packageFragmentRoot
                        .createPackageFragment(t.getTarget(), true, null);
                final ICompilationUnit clientSideClass = targetPackage
                        .createCompilationUnit(t.getFileName(), src, false,
                                null);

                createdFiles.add((IFile) clientSideClass
                        .getCorrespondingResource());
            }

            // refresh whole thing, as we could have created stuff anywhere
            packageFragmentRoot.getResource().refreshLocal(
                    IResource.DEPTH_INFINITE, monitor);

        } catch (JavaModelException e) {
            throw ErrorUtil.newCoreException(
                    "Failed to create client side class", e);
        } catch (InstantiationException e) {
            throw ErrorUtil.newCoreException("Failed to instantiate template",
                    e);
        } catch (IllegalAccessException e) {
            throw ErrorUtil.newCoreException("IllegalAccess (plugin problem)",
                    e);
        }

    }

    /**
     * Opens created files in the IDE
     */
    private void openFiles() {
        getShell().getDisplay().asyncExec(new Runnable() {
            public void run() {
                IWorkbenchPage wbPage = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getActivePage();
                try {
                    for (IFile file : createdFiles) {
                        IDE.openEditor(wbPage, file);
                    }
                } catch (PartInitException e) {
                    ErrorUtil.handleBackgroundException(IStatus.WARNING,
                            "Failed to open created files in editor", e);
                }
            }
        });

    }

    /**
     * We will accept the selection in the workbench to see if we can initialize
     * from it.
     * 
     * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
     */
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.selection = selection;
    }

    @Override
    public boolean canFinish() {
        return super.canFinish() && page.getProject() != null;
    }
}
