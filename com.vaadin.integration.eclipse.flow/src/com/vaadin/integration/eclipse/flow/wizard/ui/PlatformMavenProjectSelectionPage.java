package com.vaadin.integration.eclipse.flow.wizard.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import com.vaadin.integration.eclipse.flow.FlowPlugin;
import com.vaadin.integration.eclipse.flow.util.LogUtil;
import com.vaadin.integration.eclipse.flow.wizard.Starter;
import com.vaadin.integration.eclipse.flow.wizard.TechStack;

public class PlatformMavenProjectSelectionPage extends WizardPage {

    private static final int WIDTH = 650;
    private static final int HEIGHT = 500;

    private PlatformStarterSelectionComposite starterSelectionComposite;

    public PlatformMavenProjectSelectionPage() {
        super("Vaadin 10+ project");
        configureImg();
        setTitle("Vaadin 10+ project");
        setDescription(
                "Select a Maven project type.\nPlease note that Vaadin 14 is still a pre-release version.");
    }

    @Override
    public void createControl(Composite parent) {
        starterSelectionComposite = new PlatformStarterSelectionComposite(
                parent);
        setControl(starterSelectionComposite);
        init();
        setSize();
    }

    public void init() {
        try {
            starterSelectionComposite.init();
            updateStatus(null, false);
        } catch (Exception e) {
            LogUtil.handleBackgroundException(
                    "Can't fetch data from the start service", e);
            updateStatus("Unable to fetch information about available starters",
                    true);
        }
    }

    private void setSize() {
        getShell().setSize(WIDTH, HEIGHT);
    }

    private void updateStatus(String errorMessage, boolean errorHappened) {
        setErrorMessage(errorMessage);
        setPageComplete(!errorHappened);
    }

    private void configureImg() {
        ImageRegistry registry = FlowPlugin.getInstance().getImageRegistry();
        Image wizardBannerIcon = registry.get(FlowPlugin.VAADIN_PROJECT_IMG);
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

    public TechStack getStack() {
        return starterSelectionComposite.getStack();
    }
}
