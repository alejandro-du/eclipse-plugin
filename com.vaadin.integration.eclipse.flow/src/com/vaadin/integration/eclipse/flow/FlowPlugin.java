package com.vaadin.integration.eclipse.flow;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;

public class FlowPlugin extends AbstractUIPlugin {

    public static final String ID = "com.vaadin.integration.eclipse.flow";
    public static final String VAADIN_PROJECT_IMG = "icons.new-platform-maven-project-wizard-banner";

    private static FlowPlugin INSTANCE;

    public FlowPlugin() {
        INSTANCE = this;
    }

    public static FlowPlugin getInstance() {
        return INSTANCE;
    }

    @Override
    protected void initializeImageRegistry(ImageRegistry registry) {
        super.initializeImageRegistry(registry);

        Bundle bundle = Platform.getBundle(ID);
        IPath path = new Path("icons/flow-logo-64.png");
        URL url = FileLocator.find(bundle, path, null);
        ImageDescriptor desc = ImageDescriptor.createFromURL(url);
        registry.put(VAADIN_PROJECT_IMG, desc);
    }
}
