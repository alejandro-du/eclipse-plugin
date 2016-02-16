package com.vaadin.integration.eclipse.wizards;

import com.vaadin.integration.eclipse.wizards.Vaadin7MavenProjectWizard.VaadinArchetype;

/**
 * Simple callback interface for receiving information about selected
 * {@link VaadinArchetype}.
 */
public interface ArchetypeSelectionCallback {

    /**
     * Called when {@link VaadinArchetype} has been selected.
     *
     * @param archetype
     *            selected vaadin archetype
     */
    void onArchetypeSelect(VaadinArchetype archetype);
}
