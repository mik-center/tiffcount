// Licensed under the Apache License, Version 2.0
// See http://www.apache.org/licenses/LICENSE-2.0

package de.mik_center.lib.gui;

public class RadioButtonChoice<T> extends FormElement {

    private T initialValue;

    public RadioButtonChoice(Form form, T initialValue) {
        super(form);
        this.initialValue = initialValue;
    }
}
