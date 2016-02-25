package com.vaadin.integration.eclipse.wizards;

import java.util.List;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.vaadin.integration.eclipse.wizards.Vaadin7MavenProjectWizard.VaadinArchetype;

public class Vaadin7MavenProjectArchetypeSelectionPage extends WizardPage {

    private final List<VaadinArchetype> vaadinArchetypes;
    private Vaadin7MavenProjectArchetypeSelectionView selectionView;

    protected Vaadin7MavenProjectArchetypeSelectionPage(
            List<VaadinArchetype> vaadinArchetypes) {
        super("Select Project Type");

        this.vaadinArchetypes = vaadinArchetypes;
    }

    public void createControl(Composite parent) {
        // set by the wizard for all pages
        // setTitle(Vaadin7MavenProjectWizard.WIZARD_PAGE_TITLE);

        setDescription("Select a Maven archetype");
        selectionView = new Vaadin7MavenProjectArchetypeSelectionView(
                vaadinArchetypes, parent, SWT.NONE);
        setControl(selectionView);
    }

    @Override
    public boolean isPageComplete() {
        return getVaadinArchetype() != null && super.isPageComplete();
    }

    public VaadinArchetype getVaadinArchetype() {
        return selectionView.getVaadinArchetype();
    }

    public void selectVaadinArchetype(VaadinArchetype archetype) {
        selectionView.selectVaadinArchetype(archetype);
    }
}
