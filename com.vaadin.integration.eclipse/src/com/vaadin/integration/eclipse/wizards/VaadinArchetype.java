package com.vaadin.integration.eclipse.wizards;

import org.apache.maven.archetype.catalog.Archetype;

public class VaadinArchetype {

    private static final String PRERELEASE_REPOSITORY_URL = "https://maven.vaadin.com/vaadin-prereleases/";

    private final String title;
    private final String description;
    private boolean prerelease;
    private final Archetype archetype;

    public VaadinArchetype(String title, Archetype archetype,
            String description) {
        this.title = title;
        this.archetype = archetype;
        this.description = description;
    }

    public VaadinArchetype(String title, String artifactId, String groupId,
            String version, String description, boolean prerelease) {
        this.title = title;
        archetype = new Archetype();
        archetype.setArtifactId(artifactId);
        archetype.setGroupId(groupId);
        archetype.setVersion(version);
        this.description = description;
        this.prerelease = prerelease;
        if (prerelease) {
            archetype.setRepository(PRERELEASE_REPOSITORY_URL);
        }
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPrerelease() {
        return prerelease;
    }

    public Archetype getArchetype() {
        return archetype;
    }
}
