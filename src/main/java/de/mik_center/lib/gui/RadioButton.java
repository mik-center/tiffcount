// Licensed under the Apache License, Version 2.0
// See http://www.apache.org/licenses/LICENSE-2.0

package de.mik_center.lib.gui;

public class RadioButton<T> extends FormElement {

    public RadioButton(RadioButtonChoice<T> choice, int left, int top, int width, int height, T value) {
        super(choice.form);
    }
}
