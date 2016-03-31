package com.vaadin.integration.eclipse;

import java.net.URL;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class VaadinPlugin extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "com.vaadin.integration.eclipse";

    public static final String VAADIN_PACKAGE_PREFIX = "com.vaadin.";

    // Vaadin 6 Application class
    public static final String APPLICATION_CLASS_NAME = "Application";
    public static final String APPLICATION_CLASS_FULL_NAME = VAADIN_PACKAGE_PREFIX
            + APPLICATION_CLASS_NAME;

    // Vaadin 7 UI class
    public static final String VAADIN_UI_PACKAGE_PREFIX = VAADIN_PACKAGE_PREFIX
            + "ui.";
    public static final String UI_CLASS_NAME = "UI";
    public static final String UI_CLASS_FULL_NAME = VAADIN_UI_PACKAGE_PREFIX
            + UI_CLASS_NAME;

    // Vaadin 7 server/servlet classes
    public static final String VAADIN_SERVER_PACKAGE_PREFIX = VAADIN_PACKAGE_PREFIX
            + "server.";
    public static final String VAADIN_SERVLET_CLASS_NAME = "VaadinServlet";
    public static final String VAADIN_SERVLET_CLASS_FULL_NAME = VAADIN_SERVER_PACKAGE_PREFIX
            + VAADIN_SERVLET_CLASS_NAME;

    // Vaadin 7 annotations
    public static final String VAADIN_ANNOTATIONS_PACKAGE_PREFIX = VAADIN_PACKAGE_PREFIX
            + "annotations.";
    public static final String THEME_ANNOTATION_NAME = "Theme";
    public static final String THEME_ANNOTATION_FULL_NAME = VAADIN_ANNOTATIONS_PACKAGE_PREFIX
            + THEME_ANNOTATION_NAME;
    public static final String VAADIN_SERVLET_CONFIGURATION_ANNOTATION_NAME = "VaadinServletConfiguration";
    public static final String VAADIN_SERVLET_CONFIGURATION_ANNOTATION_FULL_NAME = VAADIN_ANNOTATIONS_PACKAGE_PREFIX
            + VAADIN_SERVLET_CONFIGURATION_ANNOTATION_NAME;

    public static final String VAADIN_SERVLET_CONFIGURATION_ANNOTATION_PARAMETER_WIDGETSET = "widgetset";

    public static final String THEME_FOLDER_NAME = "themes";

    public static final String TEST_FOLDER_NAME = "test";

    public static final String THEME_ADDON_IMPORTS = "addons.scss";

    public static final String ID_COMPILE_WS_APP = "compilews";

    public static final String VAADIN_CLIENT_SIDE_CLASS_PREFIX = "V";

    public static final String VAADIN_RESOURCE_DIRECTORY = "VAADIN";

    public static final String VAADIN_ADDON_THEME_DIRECTORY = "addons";

    public static final String VAADIN_DEFAULT_THEME = "reindeer";
    public static final String VAADIN_73_DEFAULT_THEME = "valo";

    public static final String GWT_COMPILER_CLASS = "com.vaadin.tools.WidgetsetCompiler";

    public static final String GWT_CODE_SERVER_CLASS = "com.google.gwt.dev.codeserver.CodeServer";

    public static final String GWT_OLD_COMPILER_CLASS = "com.google.gwt.dev.GWTCompiler";

    public static final String ADDON_IMPORTER_CLASS = "com.vaadin.server.themeutils.SASSAddonImportFileCreator";
    public static final String THEME_COMPILER_CLASS = "com.vaadin.sass.SassCompiler";

    // image IDs for the plugin shared image registry
    public static final String COMPILE_WIDGETSET_IMAGE_ID = "icons.compile-widgetset";
    public static final String COMPILE_THEME_IMAGE_ID = "icons.compile-theme";
    public static final String COMPILE_WIDGETSET_AND_THEME_IMAGE_ID = "icons.compile-widgetset-and-theme";
    public static final String NEW_MAVEN_PROJECT_WIZARD_BANNER_IMAGE_ID = "icons.new-vaadin-maven-project-wizard-banner";

    public static final String COMPILE_ACTION_WIDGETSET = "widgetset";
    public static final String COMPILE_ACTION_THEME = "theme";
    public static final String COMPILE_ACTION_BOTH = "both";

    private static VaadinPlugin instance = null;

    public VaadinPlugin() {
        instance = this;
    }

    public static VaadinPlugin getInstance() {
        return instance;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        // Listen to new projects in order to add AddonStylesBuilder on import
        // when necessary (#15500).
        ResourcesPlugin.getWorkspace().addResourceChangeListener(
                new NewProjectListener(), IResourceChangeEvent.POST_BUILD);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
    }

    @Override
    public ImageRegistry getImageRegistry() {
        return LazyInitializer.IMAGE_REGISTRY;
    }

    @Override
    protected void initializeImageRegistry(ImageRegistry registry) {
        super.initializeImageRegistry(registry);

        Bundle bundle = Platform.getBundle(PLUGIN_ID);

        IPath path = new Path("icons/compile-widgetset-16.png");
        URL url = FileLocator.find(bundle, path, null);
        ImageDescriptor desc = ImageDescriptor.createFromURL(url);
        registry.put(COMPILE_WIDGETSET_IMAGE_ID, desc);

        path = new Path("icons/compile-theme-16.png");
        url = FileLocator.find(bundle, path, null);
        desc = ImageDescriptor.createFromURL(url);
        registry.put(COMPILE_THEME_IMAGE_ID, desc);

        path = new Path("icons/compile-widgetset-and-theme-16.png");
        url = FileLocator.find(bundle, path, null);
        desc = ImageDescriptor.createFromURL(url);
        registry.put(COMPILE_WIDGETSET_AND_THEME_IMAGE_ID, desc);

        path = new Path("icons/vaadin-logo-white-56.png");
        url = FileLocator.find(bundle, path, null);
        desc = ImageDescriptor.createFromURL(url);
        registry.put(NEW_MAVEN_PROJECT_WIZARD_BANNER_IMAGE_ID, desc);
    }

    private synchronized ImageRegistry doGetImageRegistry() {
        return super.getImageRegistry();
    }

    private static class LazyInitializer {
        // Image registry is not thread safe, wrap it in "synchronized" delegate
        private static final ImageRegistry IMAGE_REGISTRY = new ImageRegistryDelegate(
                VaadinPlugin.getInstance().doGetImageRegistry());
    }

}