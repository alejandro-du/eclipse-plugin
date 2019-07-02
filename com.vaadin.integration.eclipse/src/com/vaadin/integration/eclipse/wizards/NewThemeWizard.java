package com.vaadin.integration.eclipse.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.builder.AddonStylesImporter;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.ProjectUtil;
import com.vaadin.integration.eclipse.util.ThemesUtil;

/**
 * Wizard to create new theme. If project is selected, it is created there,
 * otherwise to first web project.
 * 
 * Created with eclipse template.
 */

public class NewThemeWizard extends Wizard implements INewWizard {
    private NewThemeWizardPage page;
    private ISelection selection;
    private MethodInvocation setThemeMethod;
    // TODO might need to support also NormalAnnotation in the future
    private SingleMemberAnnotation themeAnnotation;
    private List<IFile> modifiedJavaFiles;

    /**
     * Constructor for NewThemeWizard.
     */
    public NewThemeWizard() {
        super();
        setWindowTitle("New Vaadin theme");
        setNeedsProgressMonitor(true);
    }

    /**
     * Adding the page to the wizard.
     */

    @Override
    public void addPages() {
        page = new NewThemeWizardPage(selection);
        addPage(page);
    }

    /**
     * This method is called when 'Finish' button is pressed in the wizard. We
     * will create an operation and run it using wizard as execution context.
     */
    @Override
    public boolean performFinish() {
        // these need to be accessed in the SWT UI thread
        final String themeName = page.getThemeName();
        // this can contain Application (Vaadin 6) and/or UI (Vaadin 7)
        // instances
        final List<IType> appOrUiClassesToModify = page
                .getApplicationAndUiClassesToModify();
        IRunnableWithProgress op = new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor)
                    throws InvocationTargetException {
                try {
                    doFinish(themeName, appOrUiClassesToModify, monitor);
                } catch (CoreException e) {
                    throw new InvocationTargetException(e);
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
    private void doFinish(final String themeName,
            List<IType> appOrUiClassesToModify, IProgressMonitor monitor)
            throws CoreException {
        // create a sample file
        try {
            monitor.beginTask("Creating " + themeName,
                    3 + 5 * appOrUiClassesToModify.size());
            IProject project = page.getProject();

            IJavaProject jproject = JavaCore.create(project);
            IType uiType = jproject.findType(VaadinPlugin.UI_CLASS_FULL_NAME);
            boolean scssTheme = uiType != null;

            double version = ProjectUtil.getVaadinVersion(project);
            boolean isValoSupported = false;
            if (version >= 7.3d) {
                isValoSupported = true;
            }

            final IFile[] files = ThemesUtil.createTheme(jproject, themeName,
                    scssTheme, new SubProgressMonitor(monitor, 1),
                    AddonStylesImporter.isSupported(project), isValoSupported);

            monitor.setTaskName("Modifying Java file(s) to use theme...");
            // update selected application/UI classes
            modifiedJavaFiles = new ArrayList<IFile>();
            for (IType appOrUi : appOrUiClassesToModify) {
                if (null == appOrUi) {
                    // should not happen
                    monitor.worked(3);
                    continue;
                }
                // is this a UI class or an application class?
                boolean isUi = false;
                if (uiType != null) {
                    ITypeHierarchy typeHierarchy = appOrUi
                            .newTypeHierarchy(new SubProgressMonitor(monitor, 2));
                    isUi = typeHierarchy.contains(uiType);
                }

                if (isUi) {
                    modifyUiForTheme(project, appOrUi, themeName,
                            new SubProgressMonitor(monitor, 3));
                } else {
                    modifyApplicationForTheme(project, appOrUi, themeName,
                            new SubProgressMonitor(monitor, 3));
                }
            }

            monitor.worked(1);
            monitor.setTaskName("Opening file(s) for editing...");
            getShell().getDisplay().asyncExec(new Runnable() {
                public void run() {
                    IWorkbenchPage page = PlatformUI.getWorkbench()
                            .getActiveWorkbenchWindow().getActivePage();
                    try {
                        for (IFile file : files) {
                            IDE.openEditor(page, file, true);
                        }
                        for (IFile file : modifiedJavaFiles) {
                            IDE.openEditor(page, file);
                        }
                    } catch (PartInitException e) {
                    }
                }
            });
            monitor.worked(1);
        } finally {
            monitor.done();
        }
    }

    @SuppressWarnings("unchecked")
    private void modifyApplicationForTheme(IProject project, IType app,
            final String themeName, IProgressMonitor monitor)
            throws JavaModelException {
        try {
            monitor.beginTask("Modifying application class", 2);

            ICompilationUnit compilationUnit = app.getCompilationUnit();

            IFile javaFile = (IFile) compilationUnit.getCorrespondingResource();
            if (javaFile != null) {
                modifiedJavaFiles.add(javaFile);
            }

            String source = compilationUnit.getSource();
            Document document = new Document(source);

            compilationUnit
                    .becomeWorkingCopy(new SubProgressMonitor(monitor, 1));
            IMethod method = app.getMethod("init", new String[] {});
            if (method != null) {
                ASTParser parser = ASTParser.newParser(AST.JLS3);
                parser.setSource(compilationUnit);
                parser.setResolveBindings(true);
                CompilationUnit astRoot = (CompilationUnit) parser
                        .createAST(new SubProgressMonitor(monitor, 1));

                astRoot.recordModifications();
                final AST ast = astRoot.getAST();

                setThemeMethod = null;

                astRoot.accept(new ASTVisitor() {
                    @Override
                    public boolean visit(MethodInvocation node) {
                        String name = node.getName().toString();
                        if (name.equals("setTheme")) {
                            // found an existing setTheme call
                            setThemeMethod = node;
                        }
                        return super.visit(node);
                    }
                });

                final StringLiteral themeString = ast.newStringLiteral();
                themeString.setLiteralValue(themeName);

                if (setThemeMethod != null) {
                    // UPDATE existing
                    setThemeMethod.arguments().clear();
                    setThemeMethod.arguments().add(themeString);
                } else {
                    // ADD setTheme method
                    astRoot.accept(new ASTVisitor() {
                        @Override
                        public boolean visit(MethodDeclaration node) {
                            String name = node.getName().toString();
                            if (name.equals("init")) {
                                Block body = node.getBody();
                                MethodInvocation setThemeInvocation = ast
                                        .newMethodInvocation();
                                setThemeInvocation.setName(ast
                                        .newSimpleName("setTheme"));
                                setThemeInvocation.arguments().add(themeString);
                                ExpressionStatement es = ast
                                        .newExpressionStatement(setThemeInvocation);

                                body.statements().add(es);
                            }
                            return super.visit(node);
                        }
                    });
                }

                TextEdit rewrite = astRoot.rewrite(document, compilationUnit
                        .getJavaProject().getOptions(true));
                try {
                    rewrite.apply(document);
                } catch (MalformedTreeException e) {
                    ErrorUtil.handleBackgroundException(IStatus.WARNING,
                            "Failed to set the theme in the application class "
                                    + app.getFullyQualifiedName(), e);
                } catch (BadLocationException e) {
                    ErrorUtil.handleBackgroundException(IStatus.WARNING,
                            "Failed to set the theme in the application class "
                                    + app.getFullyQualifiedName(), e);
                }
                String newSource = document.get();
                compilationUnit.getBuffer().setContents(newSource);
            }
        } finally {
            monitor.done();
        }
    }

    private void modifyUiForTheme(IProject project, final IType ui,
            final String themeName, IProgressMonitor monitor)
            throws JavaModelException {
        try {

            monitor.beginTask("Modifying UI class", 5);

            ICompilationUnit compilationUnit = ui.getCompilationUnit();

            IFile javaFile = (IFile) compilationUnit.getCorrespondingResource();
            if (javaFile != null) {
                modifiedJavaFiles.add(javaFile);
            }

            String source = compilationUnit.getSource();
            Document document = new Document(source);

            compilationUnit
                    .becomeWorkingCopy(new SubProgressMonitor(monitor, 1));

            try {
                ASTParser parser = ASTParser.newParser(AST.JLS3);
                parser.setSource(compilationUnit);
                parser.setResolveBindings(true);
                CompilationUnit astRoot = (CompilationUnit) parser
                        .createAST(new SubProgressMonitor(monitor, 1));

                astRoot.recordModifications();
                final AST ast = astRoot.getAST();

                themeAnnotation = null;

                astRoot.accept(new ASTVisitor() {

                    @Override
                    public boolean visit(SingleMemberAnnotation annotation) {
                        ITypeBinding binding = annotation.resolveTypeBinding();
                        String fullyQualifiedName = binding.getQualifiedName();
                        if (VaadinPlugin.THEME_ANNOTATION_FULL_NAME
                                .equals(fullyQualifiedName)) {
                            themeAnnotation = annotation;
                            return false;
                        }
                        return super.visit(annotation);
                    }

                    // Only look at top-level annotations
                    // TODO might be suboptimal if does not stop traversal early

                    @Override
                    public boolean visit(Block node) {
                        return false;
                    }

                    @Override
                    public boolean visit(MethodDeclaration node) {
                        return false;
                    }

                    @Override
                    public boolean visit(FieldDeclaration node) {
                        return false;
                    }
                });

                final StringLiteral themeString = ast.newStringLiteral();
                themeString.setLiteralValue(themeName);

                final ImportRewrite importRewrite = ImportRewrite.create(
                        astRoot, true);

                if (themeAnnotation != null) {
                    // UPDATE existing
                    themeAnnotation.setValue(themeString);
                } else {
                    // ADD Theme annotation
                    astRoot.accept(new ASTVisitor() {
                        @SuppressWarnings("unchecked")
                        @Override
                        public boolean visit(TypeDeclaration node) {
                            // find the UI class and add @Theme annotation
                            ITypeBinding binding = node.resolveBinding();
                            if (ui.getFullyQualifiedName().equals(
                                    binding.getQualifiedName())) {
                                themeAnnotation = ast
                                        .newSingleMemberAnnotation();
                                importRewrite
                                        .addImport(VaadinPlugin.THEME_ANNOTATION_FULL_NAME);
                                themeAnnotation.setTypeName(ast
                                        .newName(VaadinPlugin.THEME_ANNOTATION_NAME));
                                themeAnnotation.setValue(themeString);
                                node.modifiers().add(0, themeAnnotation);
                            }
                            return super.visit(node);
                        }
                    });
                }

                TextEdit rewrite = astRoot.rewrite(document, compilationUnit
                        .getJavaProject().getOptions(true));
                try {
                    rewrite.apply(document);

                    // rewrite imports (after the body rewrite, modifies earlier
                    // part)
                    TextEdit rewriteImports = importRewrite
                            .rewriteImports(new SubProgressMonitor(monitor, 1));
                    rewriteImports.apply(document);
                } catch (MalformedTreeException e) {
                    ErrorUtil.handleBackgroundException(
                            IStatus.WARNING,
                            "Failed to set the theme in the UI class "
                                    + ui.getFullyQualifiedName(), e);
                } catch (BadLocationException e) {
                    ErrorUtil.handleBackgroundException(
                            IStatus.WARNING,
                            "Failed to set the theme in the UI class "
                                    + ui.getFullyQualifiedName(), e);
                } catch (CoreException e) {
                    ErrorUtil.handleBackgroundException(
                            IStatus.WARNING,
                            "Failed to update imports in the UI class "
                                    + ui.getFullyQualifiedName(), e);
                }
                String newSource = document.get();
                compilationUnit.getBuffer().setContents(newSource);

                // reconcile changes with other modifications to the same
                // compilation unit
                compilationUnit.reconcile(ICompilationUnit.NO_AST, false, null,
                        new SubProgressMonitor(monitor, 1));

                compilationUnit.commitWorkingCopy(false,
                        new SubProgressMonitor(monitor, 1));
            } finally {
                compilationUnit.discardWorkingCopy();
            }
        } finally {
            monitor.done();
        }
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