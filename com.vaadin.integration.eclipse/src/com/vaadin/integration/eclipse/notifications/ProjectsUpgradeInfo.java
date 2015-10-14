package com.vaadin.integration.eclipse.notifications;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;

import com.vaadin.integration.eclipse.util.data.AbstractVaadinVersion;
import com.vaadin.integration.eclipse.util.data.DownloadableVaadinVersion;
import com.vaadin.integration.eclipse.util.data.MavenVaadinVersion;

public class ProjectsUpgradeInfo {
    private final Map<IProject, ? extends AbstractVaadinVersion> nightlies;
    private final Map<IProject, List<MavenVaadinVersion>> vaadin7Upgrades;

    public ProjectsUpgradeInfo(
            Map<IProject, DownloadableVaadinVersion> nightlies,
            Map<IProject, List<MavenVaadinVersion>> vaadin7Upgrades) {
        this.nightlies = nightlies;
        this.vaadin7Upgrades = vaadin7Upgrades;
    }

    Map<IProject, ? extends AbstractVaadinVersion> getNightlies() {
        return nightlies;
    }

    Map<IProject, List<MavenVaadinVersion>> getUpgradeProjects() {
        return vaadin7Upgrades;
    }
}