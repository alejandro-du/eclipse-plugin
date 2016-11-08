package com.vaadin.integration.eclipse.wizards;

import java.util.List;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

public class VaadinMavenProjectArchetypeSelectionPage extends WizardPage {

    private final List<VaadinArchetype> vaadinArchetypes;
    private VaadinMavenProjectArchetypeSelectionView selectionView;

    protected VaadinMavenProjectArchetypeSelectionPage(
            List<VaadinArchetype> vaadinArchetypes) {
        super("Select Project Type");

        this.vaadinArchetypes = vaadinArchetypes;
    }

    @Override
    public void createControl(Composite parent) {
        setDescription("Select a Maven archetype");
        selectionView = new VaadinMavenProjectArchetypeSelectionView(
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
