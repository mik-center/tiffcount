// Licensed under the Apache License, Version 2.0
// See http://www.apache.org/licenses/LICENSE-2.0

package de.mik_center.lib.gui;

/**
 * Abstract parent class of all form elements.
 */
public abstract class FormElement {
    protected Form form;

    public FormElement(Form form) {
        this.form = form;
    }

    /**
     * &#x1F511; Sets the form element to enabled or disabled. If disabled, the user
     * can not interact with the form element. Typically disabled elements are
     * visualized by the GUI, i.e. grayed out. This procedure must be overloaded for
     * any elements implementing an enabled—disabled behavior. For decorative
     * elements such as texts or images, this function is typically meaningless.
     * Therefore, an empty default body is provided.
     * 
     * @param enabled true to enable, false to disable
     */
    public void setEnabled(boolean enabled) { }
}
