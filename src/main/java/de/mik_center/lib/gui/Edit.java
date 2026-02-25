// Licensed under the Apache License, Version 2.0
// See http://www.apache.org/licenses/LICENSE-2.0

package de.mik_center.lib.gui;

import static org.eclipse.swt.SWT.*;

import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;

/**
 * &#x1F511; A single-line text edit box.
 */
public class Edit extends FormElement {

    private Text edit;

    /**
     * &#x1F511; Creates a new single-line text input field. The input field is
     * added to the form and becomes visible.
     *
     * @param form   form to show the button on
     * @param left   distance from left in px
     * @param top    distance from top in px
     * @param width  width in px
     * @param height height in px
     * @param value  initial content
     */
    public Edit(Form form, int left, int top, int width, int height, String value) {
        super(form);
        this.edit = new Text (form.shell, BORDER);
        this.edit.setBounds(left, top, width, height);
        this.edit.setText(value);
    }

    /**
     * &#x1F511; Returns the value of the input field.<P>
     *
     * <B>API note:</B><BR>
     * This function may only be called from within the GUI thread.
     * 
     * @return the value from the input field
     */
    public String getValue() {
        return edit.getText();
    }

    /**
     * Sets the input field enabled or disabled. If disabled, the user can not
     * interact with the field. Typically, disabled elements are rendered grayed
     * out.<P>
     *
     * <B>API note:</B><BR>
     * The change can be requested by any thread and is passed to the GUI thread,
     * which executes it finally.
     *
     * @param enabled true to enable, false to disable
     */
    @Override
    public void setEnabled(boolean enabled) {
        if(GUI.currentThreadIsGuiThread()) {
            edit.setEnabled(enabled);
        } else {
            super.form.submitUpdateTask(() -> edit.setEnabled(enabled));
        }
    }

    /**
     * Specifies a procedure that is executed when the user changes the content of
     * the input field.
     * 
     * @param onChange procedure to execute
     */
    public void setOnChange(ModifyListener onChange) {
        edit.addModifyListener(onChange);
    }

    /**
     * Specifies the selected text.
     * 
     * @param from marking start
     * @param to   marking end
     */
    public void setSelection(int from, int to) {
        edit.setSelection(from, to);
    }

    /**
     * Changes the text of the input field.
     * 
     * @param value new content
     */
    public void setValue(String value) {
        edit.setText(value);
    }
}
