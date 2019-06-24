package com.vaadin.integration.eclipse.flow;

import java.net.URL;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.vaadin.integration.eclipse.flow.pref.JavaPreferenceHandler;
import com.vaadin.integration.eclipse.flow.pref.JavaPreferenceKey;
import com.vaadin.integration.eclipse.flow.service.AnalyticsService;

public class FlowPlugin extends AbstractUIPlugin {

    public static final String ID = "com.vaadin.integration.eclipse.flow";
    public static final String VAADIN_PROJECT_IMG = "icons.new-platform-maven-project-wizard-banner";

    private static final Pattern PROD_VERSION_PATTERN = Pattern
            .compile("\\d+\\.\\d+\\.\\d+(\\.(final|beta\\d+|alpha\\d+))?");

    private static FlowPlugin INSTANCE;

    public FlowPlugin() {
        INSTANCE = this;
    }

    public static FlowPlugin getInstance() {
        return INSTANCE;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        if (!JavaPreferenceHandler
                .getBooleanValue(JavaPreferenceKey.INSTALLED)) {
            if (AnalyticsService.track(AnalyticsService.INSTALL_EVENT_TYPE)) {
                JavaPreferenceHandler
                        .saveBooleanValue(JavaPreferenceKey.INSTALLED, true);
            }
        }
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

    public static String getVersion() {
        return Platform.getBundle(FlowPlugin.ID).getVersion().toString();
    }

    public static boolean prodMode() {
        return PROD_VERSION_PATTERN.matcher(getVersion()).matches();
    }
}
