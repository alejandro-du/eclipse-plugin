package com.vaadin.integration.eclipse.wizards;

import org.apache.maven.archetype.catalog.Archetype;

public class VaadinArchetype {

    private String title;
    private String description;
    private Archetype archetype;

    public VaadinArchetype(String title, Archetype archetype,
            String description) {
        this.title = title;
        this.archetype = archetype;
        this.description = description;
    }

    public VaadinArchetype(String title, String artifactId, String groupId,
            String version, String description) {
        this.title = title;
        archetype = new Archetype();
        archetype.setArtifactId(artifactId);
        archetype.setGroupId(groupId);
        archetype.setVersion(version);
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Archetype getArchetype() {
        return archetype;
    }
}