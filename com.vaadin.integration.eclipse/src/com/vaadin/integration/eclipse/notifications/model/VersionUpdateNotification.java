package com.vaadin.integration.eclipse.notifications.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.graphics.Image;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.notifications.Utils;
import com.vaadin.integration.eclipse.util.data.AbstractVaadinVersion;
import com.vaadin.integration.eclipse.util.data.MavenVaadinVersion;

public class VersionUpdateNotification extends Notification {

    private final Map<IProject, ? extends AbstractVaadinVersion> nightlyUpgrades;
    private final Map<IProject, List<MavenVaadinVersion>> upgrades;

    public VersionUpdateNotification(
            Map<IProject, ? extends AbstractVaadinVersion> nightlies,
            Map<IProject, List<MavenVaadinVersion>> regularUpgrades) {
        nightlyUpgrades = new WeakHashMap<IProject, AbstractVaadinVersion>(
                nightlies);
        upgrades = new WeakHashMap<IProject, List<MavenVaadinVersion>>(
                regularUpgrades);
    }

    public Map<IProject, ? extends AbstractVaadinVersion> getNightlyUpgrades() {
        return Collections.unmodifiableMap(nightlyUpgrades);
    }

    public Map<IProject, List<MavenVaadinVersion>> getUpgrades() {
        return Collections.unmodifiableMap(upgrades);
    }

    @Override
    public Image getIcon() {
        return VaadinPlugin.getInstance().getImageRegistry()
                .get(Utils.NEW_VERSIONS_ICON);
    }

    @Override
    public Image getHeaderImage() {
        return VaadinPlugin.getInstance().getImageRegistry()
                .get(Utils.NEW_VERSIONS_IMAGE);
    }

    public boolean isEmpty() {
        return getNightlyUpgrades().isEmpty() && getUpgrades().isEmpty();
    }

}
