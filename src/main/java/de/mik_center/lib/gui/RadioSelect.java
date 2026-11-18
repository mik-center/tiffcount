// Licensed under the Apache License, Version 2.0
// See http://www.apache.org/licenses/LICENSE-2.0

package de.mik_center.lib.gui;

import java.util.*;
import java.util.function.*;

import org.eclipse.swt.events.*;

/**
 * A group for toggle-button GUI elements, that mutually exclude each other.
 * <P>
 * 
 * The users can choose between several options, while all the options remain
 * visible at all times, so they can interact quickly with the application and
 * are fully informed.
 * <P>
 * 
 * @param <T> type of the result value
 */
public class RadioSelect<T> extends FormElement {

    private T initialValue;
    private T value;
    private final Collection<RadioButton<T>> members = new ArrayList<>();
    private Consumer<T> onChangeHandler;

    private final SelectionAdapter onRadioButtonClick = new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
            org.eclipse.swt.widgets.Button clickedItem = (org.eclipse.swt.widgets.Button) e.widget;
            for (RadioButton<T> member : members) {
                boolean isClicked = Objects.equals(clickedItem, member.radioButton);
                member.radioButton.setSelection(isClicked);
                if (isClicked)
                    value = member.value;
            }
            if (onChangeHandler != null)
                onChangeHandler.accept(value);
        }
    };

    /**
     * Creates a new radio select.
     * 
     * @param form         form to create the buttons on
     * @param initialValue value that is initially selected
     */
    public RadioSelect(Form form, T initialValue) {
        super(form);
        this.initialValue = initialValue;
        this.value = initialValue;
    }

    void register(RadioButton<T> radioButton) {
        org.eclipse.swt.widgets.Button buttonComponent = radioButton.radioButton;
        members.add(radioButton);
        buttonComponent.addSelectionListener(onRadioButtonClick);
        buttonComponent.setSelection(Objects.equals(initialValue, radioButton.value));
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (GUI.currentThreadIsGuiThread()) {
            for (RadioButton<T> member : members)
                member.radioButton.setEnabled(enabled);
        } else {
            for (RadioButton<T> member : members) {
                super.form.submitUpdateTask(() -> member.radioButton.setEnabled(enabled));
            }
        }
    }

    /**
     * Returns the currently selected value.
     * 
     * @return the selected value
     */
    public T getValue() {
        return value;
    }

    /**
     * Sets a listener that handles select changes.
     * 
     * @param onChangeHandler consumer to invoke on changes
     */
    public void setOnChange(Consumer<T> onChangeHandler) {
        this.onChangeHandler = onChangeHandler;
    }
}
