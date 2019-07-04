package com.vaadin.integration.eclipse.flow.wizard;

import java.util.List;

public class Starter {

    public static final String DEFAULT_PROJECT_NAME = "My Starter Project";
    public static final String DEFAULT_GROUP_ID = "com.example.test";

    private String id;
    private String release;
    private String title;
    private String subTitle;
    private String description;
    private boolean commercial;
    private boolean requiresWebComponent;
    private List<TechStack> techStacks;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRelease() {
        return release;
    }

    public void setRelease(String release) {
        this.release = release;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isCommercial() {
        return commercial;
    }

    public void setCommercial(boolean commercial) {
        this.commercial = commercial;
    }

    public boolean isRequiresWebComponent() {
        return requiresWebComponent;
    }

    public void setRequiresWebComponent(boolean requiresWebComponent) {
        this.requiresWebComponent = requiresWebComponent;
    }

    public List<TechStack> getTechStacks() {
        return techStacks;
    }

    public void setTechStacks(List<TechStack> techStacks) {
        this.techStacks = techStacks;
    }
}
