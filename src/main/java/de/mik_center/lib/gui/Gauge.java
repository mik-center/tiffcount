// Licensed under the Apache License, Version 2.0
// See http://www.apache.org/licenses/LICENSE-2.0

package de.mik_center.lib.gui;

import static org.eclipse.swt.SWT.*;

import org.eclipse.swt.widgets.*;

/**
 * &#x1F511; A progress bar.
 */
public class Gauge extends FormElement {

    private ProgressBar gauge;
    private int max;

    public Gauge(Form form, int left, int top, int width, int height, int max, int value) {
        super(form);
        this.gauge = new ProgressBar(form.shell, HORIZONTAL);
        this.gauge.setBounds(left, top, width, height);
        this.gauge.setMaximum(max);
        this.gauge.setSelection(value);
        this.max = max;
    }

    public int getMax() {
        return max;
    }

    public void setValue(int value) {
        super.form.submitUpdateTask(() -> gauge.setSelection(value));
    }
}
