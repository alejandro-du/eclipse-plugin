package com.vaadin.integration.eclipse.wizards.platform;

import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import com.vaadin.integration.eclipse.util.network.platform.Starter;
import com.vaadin.integration.eclipse.util.network.platform.StarterManager;

public class PlatformMavenProjectWizard extends Wizard implements INewWizard {

    private PlatformMavenProjectSelectionPage projectSelectionPage;

    public PlatformMavenProjectWizard() {
        setWindowTitle("New Vaadin Project");
        setHelpAvailable(false);
    }

    @Override
    public void addPages() {
        projectSelectionPage = new PlatformMavenProjectSelectionPage();
        addPage(projectSelectionPage);
    }

    @Override
    public boolean performFinish() {
        final Starter starter = projectSelectionPage.getStarter();
        final String projectName = projectSelectionPage.getProjectName();
        final String groupId = projectSelectionPage.getGroupId();
        final String stack = projectSelectionPage.getStack();
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                StarterManager.scheduleStarterImport(starter, projectName,
                        groupId, stack);
            }
        });
        return true;
    }

    @Override
    public void setContainer(IWizardContainer wizardContainer) {
        super.setContainer(wizardContainer);
        if (wizardContainer == null) {
            return;
        }
        IPageChangeProvider pageChangeProvider = (IPageChangeProvider) wizardContainer;
        pageChangeProvider.addPageChangedListener(new IPageChangedListener() {
            @Override
            public void pageChanged(PageChangedEvent event) {
                if (event.getSelectedPage() == projectSelectionPage) {
                    if (!projectSelectionPage.isPageComplete()) {
                        projectSelectionPage.init();
                    }
                }
            }
        });
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        // Do nothing
    }
}
