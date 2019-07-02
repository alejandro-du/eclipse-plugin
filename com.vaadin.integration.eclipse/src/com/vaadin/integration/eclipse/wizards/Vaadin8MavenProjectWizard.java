package com.vaadin.integration.eclipse.wizards;

import java.util.List;

import org.eclipse.jface.wizard.IWizardPage;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.preferences.PreferenceConstants;
import com.vaadin.integration.eclipse.util.network.MavenVersionManager;

@SuppressWarnings("restriction")
public class Vaadin8MavenProjectWizard extends VaadinMavenProjectWizard {

    public static final String WIZARD_PAGE_TITLE = "Vaadin 8 project with Maven";

    @Override
    public void addPages() {
        super.addPages();
        for (IWizardPage page : getPages()) {
            page.setTitle(WIZARD_PAGE_TITLE);
        }
    }

    @Override
    protected List<VaadinArchetype> getAvailableArchetypes() {
        boolean includePrereleases = VaadinPlugin.getInstance()
                .getPreferenceStore()
                .getBoolean(PreferenceConstants.PRERELEASE_ARCHETYPES_ENABLED);
        List<VaadinArchetype> vaadinArchetypes = MavenVersionManager
                .getAvailableArchetypes(includePrereleases, "8\\..*");
        // TODO remove this hack once Vaadin 8 has been released
        if (vaadinArchetypes.isEmpty() && includePrereleases == false) {
            vaadinArchetypes = MavenVersionManager.getAvailableArchetypes(true,
                    "8\\..*");
        }
        return vaadinArchetypes;
    }
}
