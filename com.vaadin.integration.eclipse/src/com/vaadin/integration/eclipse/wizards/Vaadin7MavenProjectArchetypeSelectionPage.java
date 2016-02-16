package com.vaadin.integration.eclipse.wizards;

import java.util.List;

import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.vaadin.integration.eclipse.wizards.Vaadin7MavenProjectWizard.VaadinArchetype;

public class Vaadin7MavenProjectArchetypeSelectionPage extends WizardPage
        implements ArchetypeSelectionCallback {

    private final List<VaadinArchetype> vaadinArchetypes;
    private final ArchetypeSelectionCallback callback;
    private VaadinArchetype selectedArchetype;

    protected Vaadin7MavenProjectArchetypeSelectionPage(
            ArchetypeSelectionCallback callback,
            List<VaadinArchetype> vaadinArchetypes) {
        super("Select Project Type");

        this.callback = callback;
        this.vaadinArchetypes = vaadinArchetypes;
        this.selectedArchetype = vaadinArchetypes.get(0);
    }

    public void onArchetypeSelect(VaadinArchetype archetype) {
        selectedArchetype = archetype;

        callback.onArchetypeSelect(archetype);

        // TODO: This should be removed once selected archetype highlighting is
        // implemented
        IWizardContainer container = getContainer();
        IWizardPage nextPage = getNextPage();
        if (container != null && nextPage != null) {
            container.showPage(nextPage);
        }
    }

    public void createControl(Composite parent) {
        setTitle("Create New Vaadin 7 Maven Project");
        setDescription("Select a project template from the list below");
        setControl(new Vaadin7MavenProjectArchetypeSelectionView(this,
                vaadinArchetypes, parent, SWT.NONE));
    }

    @Override
    public boolean isPageComplete() {
        return selectedArchetype != null && super.isPageComplete();
    }

    public VaadinArchetype getVaadinArchetype() {
        return selectedArchetype;
    }
}
