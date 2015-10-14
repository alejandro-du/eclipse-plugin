package com.vaadin.integration.eclipse.notifications.model;

import java.util.Date;

import org.eclipse.swt.graphics.Image;

import com.vaadin.integration.eclipse.VaadinPlugin;

/**
 * Data model for notification info
 *
 */
public class Notification implements Cloneable {

    private String id;
    private String title;
    private Date date;

    private String description;
    private String category;
    private String link;
    private String linkText;

    private String icon;
    private String image;

    private boolean isRead;

    protected Notification() {
    }

    public String getTitle() {
        return title;
    }

    public Date getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public String getLink() {
        return link;
    }

    public boolean isRead() {
        return isRead;
    }

    public Image getIcon() {
        return getImage(getIconUrl());
    }

    public Image getHeaderImage() {
        return getImage(getImageUrl());
    }

    public String getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public String getLinkText() {
        return linkText;
    }

    public String getImageUrl() {
        return image;
    }

    public String getIconUrl() {
        return icon;
    }

    public void setRead() {
        isRead = true;
    }

    @Override
    protected Notification clone() {
        try {
            return (Notification) super.clone();
        } catch (CloneNotSupportedException e) {
            // This should not happen
            throw new RuntimeException("Implementation error. "
                    + "Class should implement Cloneable interface");
        }
    }

    private Image getImage(String url) {
        return url == null ? null
                : VaadinPlugin.getInstance().getImageRegistry().get(url);
    }

    public static class Builder {
        private final Notification notification;

        public Builder() {
            notification = new Notification();
        }

        public Builder setTitle(String title) {
            notification.title = title;
            return this;
        }

        public Builder setDate(Date date) {
            notification.date = date;
            return this;
        }

        public Builder setDescription(String description) {
            notification.description = description;
            return this;
        }

        public Builder setLink(String link) {
            notification.link = link;
            return this;
        }

        public Builder setRead(boolean read) {
            notification.isRead = read;
            return this;
        }

        public Builder setIcon(String iconUrl) {
            notification.icon = iconUrl;
            return this;
        }

        public Builder setImageUrl(String imageUrl) {
            notification.image = imageUrl;
            return this;
        }

        public Builder setId(String id) {
            notification.id = id;
            return this;
        }

        public Builder setCategory(String category) {
            notification.category = category;
            return this;
        }

        public Builder setLinkText(String text) {
            notification.linkText = text;
            return this;
        }

        public Notification build() {
            return notification.clone();
        }
    }

}
