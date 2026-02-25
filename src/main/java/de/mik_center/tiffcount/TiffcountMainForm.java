// Licensed under the Apache License, Version 2.0
// See http://www.apache.org/licenses/LICENSE-2.0

package de.mik_center.tiffcount;

import static java.lang.System.lineSeparator;

import de.mik_center.lib.gui.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;

/**
 * &#x1F511; The Tiffcount program window. This is the main class of the program.
 */
public final class TiffcountMainForm extends Form {

    private static final String PROGRAM_CAPTION = "MIK * Tiffcount v. 2.0.3";

    private final Edit startingPathEdit;
    private final ImageButton startPathPickerButton;
    private final Edit reportsPathEdit;
    private final ImageButton reportsPathPickerButton;
    private final Edit fileNamePatternEdit;
    private final Button analyzeFoldersButton;
    private final Button countFoldersAndFilesButton;
    private final Gauge progressBar;

    private String reportsPathAutomaticValue;

    public TiffcountMainForm() {
        super(PROGRAM_CAPTION, false);
        super.setInnerSize(s(112), s(69.28));

        new Label(this, s(9), s(9), s(87), s(3), "Startverzeichnis:");

        reportsPathAutomaticValue = System.getProperty("user.dir");
        startingPathEdit = new Edit(this, s(9), s(12), s(87), s(4), reportsPathAutomaticValue);
        startingPathEdit.setOnChange(this::startingPathEditChange);
        startingPathEdit.setSelection(0, startingPathEdit.getValue().length());

        startPathPickerButton = new ImageButton(this, s(99), s(12), s(4), s(4), "/pipette.png");
        startPathPickerButton.setOnClick(this::startPathPickerButtonClick);

        new Label(this, s(9), s(19), s(87), s(3), "Verzeichnis für den Bericht:");

        reportsPathEdit = new Edit(this, s(9), s(22), s(87), s(4), reportsPathAutomaticValue);
        reportsPathEdit.setSelection(0, reportsPathEdit.getValue().length());

        reportsPathPickerButton = new ImageButton(this, s(99), s(22), s(4), s(4), "/pipette.png");
        reportsPathPickerButton.setOnClick(this::reportsPathPickerButtonClick);

        new Label(this, s(9), s(29), s(94), s(3), "Dateien auswerten:");

        fileNamePatternEdit = new Edit(this, s(9), s(32), s(94), s(4), "*.jpg, *.tif, *.jp2, *.gif, *.png, *.pdf");

        analyzeFoldersButton = new Button(this, s(9), s(42), s(45), s(6), "Nur Ordner analysieren");
        analyzeFoldersButton.setOnClick(this::analyzeFoldersButtonClick);

        countFoldersAndFilesButton = new Button(this, s(58), s(42), s(45), s(6), "Dateien und Ordner zählen");
        countFoldersAndFilesButton.setOnClick(this::countFoldersAndFilesButtonClick);

        progressBar = new Gauge(this, s(9), s(55), s(94), s(4), 385, 0);
    }

    /* Scales the specified size to a value in pixels. This function is final
     * because it is used in the constructor. A single-character function name is
     * used here for constructor brevity. */
    private static final int s(double length) {
        assert length >= 0 : "'length' must not be negative, was " + length;

        return (int) Math.round(7 * length);
    }

    /**
     * &#x1F511; The main method, called by Java to start the program.
     * 
     * @param args command line arguments, ignored
     */
    public static void main(String[] args) {
        Form instance = new TiffcountMainForm();
        instance.showModal();
    }

    /**
     * Procedure called by the GUI when the content of the starting path edit
     * changes. As long as a directory for the reports has not yet been specifically
     * defined, that path will be updated so that the user does not have to enter
     * the path twice if it is the same.
     */
    protected void startingPathEditChange(ModifyEvent event) {
        if(reportsPathAutomaticValue.equals(reportsPathEdit.getValue())) {
            reportsPathAutomaticValue = startingPathEdit.getValue();
            reportsPathEdit.setValue(reportsPathAutomaticValue);
            reportsPathEdit.setSelection(0, reportsPathAutomaticValue.length());
        }
    }

    /**
     * Procedure called by the GUI when the start path picker button was clicked. A
     * dialog box appears in which the user can select the folder using the pointing
     * device.
     */
    protected void startPathPickerButtonClick(Event event) {
        DirectoryDialog dialog = new DirectoryDialog(super.shell);
        dialog.setText(PROGRAM_CAPTION);
        dialog.setMessage("Wählen Sie ein Startverzeichnis:");
        String selectedDir = dialog.open();
        if (selectedDir != null) {
            startingPathEdit.setValue(selectedDir);
            startingPathEditChange(new ModifyEvent(event));
        }
    }

    /**
     * Procedure called by the GUI when the reports path picker button was clicked.
     * A dialog box appears in which the user can select the folder using the
     * pointing device.
     */
    protected void reportsPathPickerButtonClick(Event event) {
        DirectoryDialog dialog = new DirectoryDialog(super.shell);
        dialog.setText(PROGRAM_CAPTION);
        dialog.setMessage("Wählen Sie ein Verzeichnis für den Bericht:");
        String selectedDir = dialog.open();
        if (selectedDir != null) {
            reportsPathEdit.setValue(selectedDir);
        }
    }

    /**
     * &#x1F511; Procedure called by the GUI when the analyze folders button was
     * clicked. The program is executed and each directory is examined. A directory
     * overview is then generated.
     */
    protected void analyzeFoldersButtonClick(Event event) {
        startEvaluation(false);
    }

    /**
     * &#x1F511; Procedure called by the GUI when the count folders and files button
     * was clicked. The program is executed, and each usable file is loaded and its
     * metadata extracted. Two reports are generated: a directory overview and a
     * file report.
     */
    protected void countFoldersAndFilesButtonClick(Event event) {
        startEvaluation(true);
    }

    private void startEvaluation(boolean analyzeImages) {
        Path startingPath = Paths.get(startingPathEdit.getValue());
        Path reportsPath = Paths.get(reportsPathEdit.getValue());
        Pattern fileNamePattern =  Pattern.compile(createFileFilter(fileNamePatternEdit.getValue()));

        startingPathEdit.setEnabled(false);
        startPathPickerButton.setEnabled(false);
        reportsPathEdit.setEnabled(false);
        reportsPathPickerButton.setEnabled(false);
        fileNamePatternEdit.setEnabled(false);
        analyzeFoldersButton.setEnabled(false);
        countFoldersAndFilesButton.setEnabled(false);

        Tiffcount tiffcount = new Tiffcount(startingPath, reportsPath, fileNamePattern, analyzeImages, this::updateProgress);
        Thread tiffcountThread = new Thread() {
            @Override
            public void run() {
                try {
                    tiffcount.run();
                    TiffcountMainForm.this.onComplete();
                } catch (Throwable thrown) {
                    TiffcountMainForm.this.onError(thrown);
                }
            }
        };
        tiffcountThread.start();
    }

    private static String createFileFilter(String value) {
        assert value != null : "'value' must not be null";

        String regex = Arrays.stream(value.split(","))
            .map(String::trim)
            .map(string -> string.replace(".", "\\."))
            .map(string -> string.replace("?", "."))
            .map(string -> string.replace("*", ".*"))
            .collect(Collectors.joining("|"));

        StringBuilder builder = new StringBuilder();
        for (int codePoint : regex.codePoints().toArray()) {
            String glyph = Character.toString(codePoint);
            String upper = glyph.toUpperCase();
            String lower = glyph.toLowerCase();
            if (upper.equals(lower)) {
                builder.append(glyph);
            } else {
                builder.append('[');
                builder.append(upper);
                builder.append(lower);
                builder.append(']');
            }
        }
        return builder.toString();
    }

    /**
     * Receives progress updates from the program.
     * 
     * @param progress progress as floating-point in [0&#x2009;;&#x2009;1]
     */
    public void updateProgress(double progress) {
        assert progress >= 0.0 && progress <= 1.0
            : "'progress' must be in range [0.0 .. 1.0], was " + progress;

        progressBar.setValue((int) Math.round(progressBar.getMax() * progress));
    }

    /**
     * Event handler if the Tiffcount program exited normally.
     */
    public void onComplete() {
        progressBar.setValue(0);
        startingPathEdit.setEnabled(true);
        startPathPickerButton.setEnabled(true);
        reportsPathEdit.setEnabled(true);
        reportsPathPickerButton.setEnabled(true);
        fileNamePatternEdit.setEnabled(true);
        analyzeFoldersButton.setEnabled(true);
        countFoldersAndFilesButton.setEnabled(true);
    }

    /**
     * Event handler if the Tiffcount program exited with an error. An error file is
     * written and the error is displayed to the user.
     */
    public void onError(Throwable thrown) {
        Exception errorWritingReport = null;
        Path errorFilePath = Paths.get("Programmfehler.txt").toAbsolutePath();
        try (
            FileWriter fileHandle = new FileWriter(errorFilePath.toFile())){
            BufferedWriter out = new BufferedWriter(fileHandle);
            thrown.printStackTrace(new PrintWriter(out));
            out.flush();
        } // out.close();
        catch (IOException | RuntimeException exception) {
            errorWritingReport = exception;
        }

        StringBuilder builder = new StringBuilder();
        appendThrowable(thrown, builder);
        builder.append(lineSeparator());
        builder.append(lineSeparator());
        if (errorWritingReport == null) {
            builder.append("Ein Bericht wurde in die Datei ");
            builder.append(errorFilePath);
            builder.append(" geschrieben.");
        } else {
            builder.append(" Ein Bericht konnte nicht geschrieben werden, Grund:");
            builder.append(lineSeparator());
            builder.append(lineSeparator());
            appendThrowable(errorWritingReport, builder);
        }
        String message = builder.toString();

        if (thrown instanceof Error) {
            int status = thrown.getClass().getSimpleName().hashCode() | 127;
            super.showFailThenDie("Es ist ein Fehler aufgetreten", message, status != 0 ? status : 123);
        } else {
            super.showWarning("Es ist ein Fehler aufgetreten", message);
            onComplete();
        }
    }

    private static void appendThrowable(Throwable thrown, StringBuilder builder) {
        builder.append(thrown.getClass().getSimpleName());
        if(thrown.getLocalizedMessage() != null) {
            builder.append(": ");
            builder.append(thrown.getLocalizedMessage());
        } else if(thrown.getMessage() != null) {
            builder.append(": ");
            builder.append(thrown.getMessage());
        }
    }
}
