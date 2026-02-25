// Licensed under the Apache License, Version 2.0
// See http://www.apache.org/licenses/LICENSE-2.0

package de.mik_center.lib.gui;

import static org.eclipse.swt.SWT.*;

/**
 * &#x1F511; Text that is displayed directly on the window's canvas.
 */
public class Label extends FormElement {

    private org.eclipse.swt.widgets.Label label;

    public Label(Form form, int left, int top, int width, int height, String value) {
        super(form);
        this.label = new org.eclipse.swt.widgets.Label(form.shell, NONE);
        this.label.setBounds(left, top, width, height);
        this.label.setText(value);
    }
}
