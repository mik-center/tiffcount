// Licensed under the Apache License, Version 2.0
// See http://www.apache.org/licenses/LICENSE-2.0

package de.mik_center.lib.gui;

import java.util.*;

/**
 * A bean with data for displaying a dialog box.
 */
public class MessagePacket {
    private String caption;
    private String message;
    private int icon;
    private int buttons;
    private Optional<Integer> status = Optional.empty();

    /**
     * Creates a new, initially empty message pack. The no-arg constructor is
     * allowed.
     */
    public MessagePacket() { }

    /**
     * Generates a message packet.
     * 
     * @param caption window title
     * @param message message to be displayed
     * @param icon    symbol to be displayed in the window. See
     *                {@link org.eclipse.swt.SWT} for values.
     * @param buttons buttons are available to close the dialog box. See
     *                {@link org.eclipse.swt.SWT} for values.
     */
    public MessagePacket(String caption, String message, int icon, int buttons) {
        this.caption = caption;
        this.message = message;
        this.icon = icon;
        this.buttons = buttons;
    }

    /**
     * Generates a message packet. After displaying the dialog message, the program
     * will attempt to terminate as quickly and orderly as possible and return the
     * return value via {@link System#exit(int)}.
     * 
     * @param caption window title
     * @param message message to be displayed
     * @param icon    symbol to be displayed in the window. See
     *                {@link org.eclipse.swt.SWT} for values.
     * @param buttons buttons are available to close the dialog box. See
     *                {@link org.eclipse.swt.SWT} for values.
     * @param status  return value, aka. error level
     */
    public MessagePacket(String caption, String message, int icon, int buttons, int status) {
        this.caption = caption;
        this.message = message;
        this.icon = icon;
        this.buttons = buttons;
        this.status = Optional.of(status);
    }

    public String getCaption() {
        return caption;
    }

    public String getMessage() {
        return message;
    }

    public int getIcon() {
        return icon;
    }

    public int getButtons() {
        return buttons;
    }

    public Optional<Integer> getStatus() {
        return status;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public void setButtons(int buttons) {
        this.buttons = buttons;
    }

    public void setStatus(Optional<Integer> status) {
        this.status = status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(buttons, caption, icon, message, status);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MessagePacket other = (MessagePacket) obj;
        return buttons == other.buttons && Objects.equals(caption, other.caption) && icon == other.icon
                && Objects.equals(message, other.message) && Objects.equals(status, other.status);
    }
}
