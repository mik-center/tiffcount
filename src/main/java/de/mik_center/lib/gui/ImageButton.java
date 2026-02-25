// Licensed under the Apache License, Version 2.0
// See http://www.apache.org/licenses/LICENSE-2.0

package de.mik_center.lib.gui;

/**
 * &#x1F511; A clickable button that can be decorated with an icon.
 */
public class ImageButton extends Button {

    public ImageButton(Form form, int left, int top, int width, int height, String resourceName) {
        super(form, left, top, width, height);
        button.setImage(ResourcesHandler.provideImageCaching(form, resourceName));
    }
}
