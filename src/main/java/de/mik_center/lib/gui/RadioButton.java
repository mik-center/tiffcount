// Licensed under the Apache License, Version 2.0
// See http://www.apache.org/licenses/LICENSE-2.0

package de.mik_center.lib.gui;

import static org.eclipse.swt.SWT.*;

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.Button;

public class RadioButton<T> extends FormElement {

    final T value;
    final Button radioButton;

    public RadioButton(RadioButtonChoice<T> choice, int left, int top, int width, int height, String text, T value) {
        super(choice.form);
        this.value = value;
        Composite container = new Composite(form.shell, NO_RADIO_GROUP);
        container.setBounds(left, top, width, height);
        this.radioButton = new org.eclipse.swt.widgets.Button(container, RADIO);
        radioButton.setBounds(0, 0, width, height);
        radioButton.setText(text);
        choice.register(this);
    }
}
