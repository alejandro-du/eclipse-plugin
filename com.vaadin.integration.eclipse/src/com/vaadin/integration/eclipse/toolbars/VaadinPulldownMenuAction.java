package com.vaadin.integration.eclipse.toolbars;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate;
import org.eclipse.ui.PlatformUI;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.handlers.CompileThemeAndWidgetsetHandler;
import com.vaadin.integration.eclipse.handlers.CompileThemeHandler;
import com.vaadin.integration.eclipse.handlers.CompileWidgetsetHandler;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.PreferenceUtil;
import com.vaadin.integration.eclipse.util.ProjectUtil;

/**
 * This action shows the widgetset and theme compilation related commands with a
 * drop-down button.
 * 
 * Clicking the main button itself runs the previously run sub-tool for the
 * selected project (or editor), by default runs widgetset compilation.
 */
public class VaadinPulldownMenuAction implements
        IWorkbenchWindowPulldownDelegate, IActionDelegate {

    private Menu vaadinPulldownMenu;

    public Menu getMenu(Control parent) {
        if (vaadinPulldownMenu == null) {
            // Build the menu
            vaadinPulldownMenu = createVaadinMenu(parent, vaadinPulldownMenu);
        }
        return vaadinPulldownMenu;
    }

    private static Menu createVaadinMenu(Control parent, Menu menu) {
        if (menu == null) {
            menu = new Menu(parent);

            ImageRegistry registry = VaadinPlugin.getInstance()
                    .getImageRegistry();
            Image compileWidgetsetIcon = registry
                    .get(VaadinPlugin.COMPILE_WIDGETSET_IMAGE_ID);
            Image compileThemeIcon = registry
                    .get(VaadinPlugin.COMPILE_THEME_IMAGE_ID);
            Image compileBothIcon = registry
                    .get(VaadinPlugin.COMPILE_WIDGETSET_AND_THEME_IMAGE_ID);

            // Compile widgetset
            final MenuItem widgetsetMenuItem = new MenuItem(menu, SWT.PUSH);
            widgetsetMenuItem.setText("Compile Widgetset");
            widgetsetMenuItem.setImage(compileWidgetsetIcon);

            // Handle selection
            widgetsetMenuItem.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    IWorkbenchPage activePage = PlatformUI.getWorkbench()
                            .getActiveWorkbenchWindow().getActivePage();
                    if (activePage != null) {
                        ISelection currentSelection = activePage.getSelection();
                        IEditorPart activeEditor = activePage.getActiveEditor();

                        IProject project = ProjectUtil.getProject(
                                currentSelection, activeEditor);
                        persistCompileAction(project,
                                VaadinPlugin.COMPILE_ACTION_WIDGETSET);

                        CompileWidgetsetHandler.startCompileWidgetsetJob(
                                currentSelection, activeEditor, project);
                    }
                }
            });

            // Compile theme
            final MenuItem themeMenuItem = new MenuItem(menu, SWT.PUSH);
            themeMenuItem.setText("Compile Theme");
            themeMenuItem.setImage(compileThemeIcon);

            // Handle selection
            themeMenuItem.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    IWorkbenchPage activePage = PlatformUI.getWorkbench()
                            .getActiveWorkbenchWindow().getActivePage();
                    if (activePage != null) {
                        ISelection currentSelection = activePage.getSelection();
                        IEditorPart activeEditor = activePage.getActiveEditor();

                        IProject project = ProjectUtil
                                .getProject(currentSelection, activeEditor);
                        persistCompileAction(project,
                                VaadinPlugin.COMPILE_ACTION_THEME);

                        CompileThemeHandler.startCompileThemeJob(
                                currentSelection, activeEditor, project);
                    }
                }
            });

            // Compile widgetset and theme
            final MenuItem compileBothMenuItem = new MenuItem(menu, SWT.PUSH);
            compileBothMenuItem.setText("Compile Widgetset and Theme");
            compileBothMenuItem.setImage(compileBothIcon);

            // Handle selection
            compileBothMenuItem.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    IWorkbenchPage activePage = PlatformUI.getWorkbench()
                            .getActiveWorkbenchWindow().getActivePage();
                    if (activePage != null) {
                        ISelection currentSelection = activePage.getSelection();
                        IEditorPart activeEditor = activePage.getActiveEditor();

                        IProject project = ProjectUtil.getProject(
                                currentSelection, activeEditor);
                        persistCompileAction(project,
                                VaadinPlugin.COMPILE_ACTION_BOTH);

                        CompileThemeAndWidgetsetHandler
                                .startCompileThemeAndWidgetsetJob(
                                currentSelection, activeEditor, project);
                    }
                }
            });
        } else {
            // Delete children
        }

        return menu;
    }

    protected static void persistCompileAction(IProject project,
            String compileAction) {
        if (project != null) {
            try {
                PreferenceUtil preferences = PreferenceUtil.get(project);
                preferences.setPreviousCompileAction(compileAction);
                preferences.persist();
            } catch (IOException ex) {
                ErrorUtil.handleBackgroundException(
                        "Failed to persist previous compile action preference",
                        ex);
            }
        }
    }

    public void dispose() {
        if (vaadinPulldownMenu != null) {
            vaadinPulldownMenu.dispose();
        }
    }

    public void init(IWorkbenchWindow window) {
    }

    public void run(IAction action) {
        IWorkbenchPage activePage = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage();
        if (activePage != null) {
            ISelection currentSelection = activePage.getSelection();
            IEditorPart activeEditor = activePage.getActiveEditor();

            IProject project = ProjectUtil.getProject(currentSelection,
                    activeEditor);

            if (null == project) {
                return;
            }

            // update tooltip based on action performed
            updateTooltip(action, project);

            String lastAction = PreferenceUtil.get(project)
                    .getPreviousCompileAction();
            if (VaadinPlugin.COMPILE_ACTION_THEME.equals(lastAction)) {
                CompileThemeHandler.startCompileThemeJob(currentSelection,
                        activeEditor, project);
            } else if (VaadinPlugin.COMPILE_ACTION_WIDGETSET.equals(lastAction)) {
                CompileWidgetsetHandler.startCompileWidgetsetJob(
                        currentSelection, activeEditor, project);
            } else if (VaadinPlugin.COMPILE_ACTION_BOTH.equals(lastAction)) {
                CompileThemeAndWidgetsetHandler
                        .startCompileThemeAndWidgetsetJob(currentSelection,
                                activeEditor, project);
            }
        }
    }

    public void selectionChanged(IAction action, ISelection selection) {
        IWorkbenchPage activePage = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage();
        IEditorPart activeEditor = activePage != null ? activePage
                .getActiveEditor() : null;

        IProject project = ProjectUtil.getProject(selection, activeEditor);

        updateTooltip(action, project);
    }

    private void updateTooltip(IAction action, IProject project) {
        if (null == project) {
            action.setToolTipText("Compile Theme or Widgetset");
            return;
        }

        String lastAction = PreferenceUtil.get(project)
                .getPreviousCompileAction();
        if (VaadinPlugin.COMPILE_ACTION_THEME.equals(lastAction)) {
            action.setToolTipText("Compile Theme");
        } else if (VaadinPlugin.COMPILE_ACTION_WIDGETSET.equals(lastAction)) {
            action.setToolTipText("Compile Widgetset");
        } else if (VaadinPlugin.COMPILE_ACTION_BOTH.equals(lastAction)) {
            action.setToolTipText("Compile Widgetset and Theme");
        }
    }

}