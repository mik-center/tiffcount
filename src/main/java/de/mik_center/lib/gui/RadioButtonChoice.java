// Licensed under the Apache License, Version 2.0
// See http://www.apache.org/licenses/LICENSE-2.0

package de.mik_center.lib.gui;

import java.util.*;

import org.eclipse.swt.events.*;

public class RadioButtonChoice<T> extends FormElement {

    private T initialValue;
    private T value;

    private final Collection<RadioButton<T>> members = new ArrayList<>();
    private final SelectionAdapter onRadioButtonClick = new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
            org.eclipse.swt.widgets.Button clickedItem = (org.eclipse.swt.widgets.Button) e.widget;
            for (RadioButton<T> member : members) {
                boolean isClicked = Objects.equals(clickedItem, member.radioButton);
                member.radioButton.setSelection(isClicked);
                if(isClicked) value = member.value;
            }
        }
    };

    public RadioButtonChoice(Form form, T initialValue) {
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
        if(GUI.currentThreadIsGuiThread()) {
            for (RadioButton<T> member : members)
                member.radioButton.setEnabled(enabled);
        } else {
            for (RadioButton<T> member : members) {
                super.form.submitUpdateTask(
                    () -> member.radioButton.setEnabled(enabled)
                );
            }
        }
    }

    public T getValue() {
        return value;
    }
}
