package com.vaadin.integration.eclipse.wizards.platform;

import java.io.IOException;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.util.ErrorUtil;
import com.vaadin.integration.eclipse.util.network.platform.Starter;

public class PlatformMavenProjectSelectionPage extends WizardPage {

    private PlatformStarterSelectionComposite starterSelectionComposite;

    public PlatformMavenProjectSelectionPage() {
        super("Vaadin Project");
        configureImg();
        setTitle("Vaadin Project");
        setDescription("Select a Maven project type");
    }

    @Override
    public void createControl(Composite parent) {
        starterSelectionComposite = new PlatformStarterSelectionComposite(
                parent);
        setControl(starterSelectionComposite);
        init();
    }

    public void init() {
        try {
            starterSelectionComposite.init();
            updateStatus(null, false);
        } catch (IOException e) {
            ErrorUtil.handleBackgroundException(
                    "Can't fetch data from the start service", e);
            updateStatus("Unable to fetch information about available starters",
                    true);
        }
    }

    private void updateStatus(String errorMessage, boolean errorHappened) {
        setErrorMessage(errorMessage);
        setPageComplete(!errorHappened);
    }

    private void configureImg() {
        ImageRegistry registry = VaadinPlugin.getInstance().getImageRegistry();
        Image wizardBannerIcon = registry.get(
                VaadinPlugin.NEW_PLATFORM_MAVEN_PROJECT_WIZARD_BANNER_IMAGE_ID);
        setImageDescriptor(ImageDescriptor.createFromImage(wizardBannerIcon));
    }

    public String getGroupId() {
        return starterSelectionComposite.getGroupId();
    }

    public String getProjectName() {
        return starterSelectionComposite.getProjectName();
    }

    public Starter getStarter() {
        return starterSelectionComposite.getStarter();
    }

    public String getStack() {
        return starterSelectionComposite.getStack();
    }
}
