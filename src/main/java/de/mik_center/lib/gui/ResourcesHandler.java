// Licensed under the Apache License, Version 2.0
// See http://www.apache.org/licenses/LICENSE-2.0

package de.mik_center.lib.gui;

import java.io.*;
import java.util.*;

import org.eclipse.swt.graphics.*;

/**
 * Provides resources from files in the JAR file.
 */
public final class ResourcesHandler {
    private static final Map<String,Image> imagesCache = new WeakHashMap<>();

    /**
     * Provides an image from an image resource file. It attempts to create the
     * image resource only once, even if it is used in multiple places. The resource
     * is automatically registered in SWT for manual memory release.
     * 
     * @param form         form on which the resource should be displayed
     * @param resourceName path to the resource as an absolute path, starting with a
     *                     forward slash, from the root level of the JAR file. The
     *                     path is case-sensitive.
     * @return the SWT {@link Image}
     */
    public static Image provideImageCaching(Form form, String resourceName) {
        String imageIdentifier = form.shell.getClass().getName() + '@'
                + Integer.toHexString(System.identityHashCode(form.shell)) + ':' + resourceName;
        Image imageInMemory = imagesCache.get(imageIdentifier);
        if (imageInMemory != null) return imageInMemory;

        Image loadedImage;
        try (InputStream imageData = form.getClass().getResourceAsStream(resourceName)) {
        assert imageData != null : "Resource \"" + resourceName + "\" not found";
        loadedImage = new Image(GUI.get(), imageData);
        } /* imageData.close() */ catch (IOException toWrap) {throw new UncheckedIOException(toWrap);}
        form.shell.addDisposeListener(disposeEvent -> loadedImage.dispose());
        imagesCache.put(imageIdentifier, loadedImage);
        return loadedImage;
    }
}
