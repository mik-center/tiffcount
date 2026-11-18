// Licensed under the Apache License, Version 2.0
// See http://www.apache.org/licenses/LICENSE-2.0

package de.mik_center.lib.gui;

import static org.eclipse.swt.SWT.*;

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.Button;

/**
 * &#x1F511; A toggle-button GUI element. The user can take in all the options
 * at a glance and interact with the interface quickly.<P>
 * 
 * The user can choose between several related round-button elements, while the
 * labels for all the elements remain visible at all times.<P>
 * 
 * This class extends {@code FormElement} because it has the fundamental
 * characteristics of a GUI element.
 * 
 * @param <T> type of the result value
 */
public class RadioButton<T> extends FormElement {

    final T value;
    final Button radioButton;

    /**
     * &#x1F511; the range of options from which the user can choose one.
     * 
     * @param choice the range of options from which the user can choose one. Must
     *               not be {@code null}.
     * @param left   horizontal distance of the control from the left edge of the
     *               drawing area, in pixels
     * @param top    vertical distance of the control from the top calculated edge
     *               of the drawing area, in pixels
     * @param width  width of the control on the drawing surface, in pixels. Cannot
     *               be negative.
     * @param height height of the control element on the drawing area, in pixels.
     *               Cannot be negative.
     * @param value  result value if this selection has been made by the user. If
     *               {@code null}, returns {@code null} as result.
     */
    public RadioButton(RadioSelect<T> choice, int left, int top, int width, int height, String text, T value) {
        super(choice.form);

        assert width >= 0 : "width must not be negative";
        assert height >= 0 : "height must not be negative";

        this.value = value;
        Composite container = new Composite(form.shell, NO_RADIO_GROUP);
        container.setBounds(left, top, width, height);
        this.radioButton = new org.eclipse.swt.widgets.Button(container, RADIO);
        radioButton.setBounds(0, 0, width, height);
        radioButton.setText(text);
        choice.register(this);
    }
}
