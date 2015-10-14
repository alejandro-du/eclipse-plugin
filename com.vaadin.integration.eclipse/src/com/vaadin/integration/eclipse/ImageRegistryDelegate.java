package com.vaadin.integration.eclipse;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;

/**
 * Wrap Image Registry class to make it threadsafe.
 *
 */
class ImageRegistryDelegate extends ImageRegistry {

    private final ImageRegistry original;

    private final Object lock;

    ImageRegistryDelegate(ImageRegistry registry) {
        original = registry;
        lock = new Object();
    }

    @Override
    public Image get(String key) {
        synchronized (lock) {
            return original.get(key);
        }
    }

    @Override
    public ImageDescriptor getDescriptor(String key) {
        synchronized (lock) {
            return original.getDescriptor(key);
        }
    }

    @Override
    public void put(String key, ImageDescriptor descriptor) {
        synchronized (lock) {
            original.put(key, descriptor);
        }
    }

    @Override
    public void put(String key, Image image) {
        synchronized (lock) {
            original.put(key, image);
        }
    }

    @Override
    public void remove(String key) {
        synchronized (lock) {
            original.remove(key);
        }
    }

    @Override
    public void dispose() {
        synchronized (lock) {
            original.dispose();
        }
    }

}
