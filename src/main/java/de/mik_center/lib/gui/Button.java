// Licensed under the Apache License, Version 2.0
// See http://www.apache.org/licenses/LICENSE-2.0

package de.mik_center.lib.gui;

import static org.eclipse.swt.SWT.*;

import org.eclipse.swt.widgets.*;

/**
 * &#x1F511; A clickable button.
 */
public class Button extends FormElement {
    /**
     * Contains the SWT button. This field is for access by inheriting classes,
     * specifically {@link ImageButton}.
     */
    protected final org.eclipse.swt.widgets.Button button;

    /**
     * Constructor for use by subclasses.
     * 
     * @param form   form to show the button on
     * @param left   distance from left in px
     * @param top    distance from top in px
     * @param width  width in px
     * @param height height in px
     */
    protected Button(Form form, int left, int top, int width, int height) {
        super(form);
        this.button = new org.eclipse.swt.widgets.Button(form.shell, PUSH);
        this.button.setBounds(left, top, width, height);
    }

    /**
     * &#x1F511; Creates a new clickable button. The button is added to the form and
     * becomes visible.
     * 
     * @param form   form to show the button on
     * @param left   distance from left in px
     * @param top    distance from top in px
     * @param width  width in px
     * @param height height in px
     * @param label  button label
     */
    public Button(Form form, int left, int top, int width, int height, String label) {
        this(form, left, top, width, height);
        button.setText(label);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if(GUI.currentThreadIsGuiThread()) {
            button.setEnabled(enabled);
        } else {
            super.form.submitUpdateTask(() -> button.setEnabled(enabled));
        }
    }

    /**
     * &#x1F511; Specifies the procedure that is performed when the button is
     * clicked.
     * 
     * @param onClick on click listener
     */
    public void setOnClick(Listener onClick) {
        button.addListener(Selection, onClick);
    }
}
