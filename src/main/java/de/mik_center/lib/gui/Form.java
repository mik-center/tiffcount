// Licensed under the Apache License, Version 2.0
// See http://www.apache.org/licenses/LICENSE-2.0

package de.mik_center.lib.gui;

import static java.lang.System.*;
import static org.eclipse.swt.SWT.*;

import java.io.*;
import java.util.*;

import org.eclipse.swt.widgets.*;

/**
 * &#x1F511; A program window.
 */
public abstract class Form {
    /**
     * The number of pixels used by the GUI from the window’s height, those pixels
     * needed to render the window frame. The value was initially determined by
     * trial and represent the behavior of Windows 11.
     */
    private static final int SWALLOWED_HEIGHT = 47;

    /**
     * The number of pixels used by the GUI from the window’s width, those pixels
     * needed to render the window frame. The value was initially determined by
     * trial and represent the behavior of Windows 11.
     */
    private static final int SWALLOWED_WIDTH = 18;

    /**
     * This is a flag and is 0 during program runtime. If it changes to a non-0
     * value, the GUI response thread is terminated as soon as possible, executes
     * System.exit(), and returns this value as exit status.
     */
    private static int exitStatus = 0;

    /**
     * Tracks whether this form is the program's main form, i.e., the lowest-level,
     * first-running form. These function classes expect the application to have
     * exactly one main form, which is created first and closed last, which is true
     * for the majority of GUI applications.
     */
    private final boolean main;

    /**
     * Holds the {@link Shell} (the SWT window handle) required by inheriting window
     * implementations to display advanced dialogs.
     */
    protected final Shell shell;

    private final Deque<Runnable> updateTasksDeque = new LinkedList<>();
    private final Deque<MessagePacket> messagesDeque = new LinkedList<>();

    /**
     * &#x1F511; Creates a new application window.
     * 
     * @param title window title
     */
    public Form(String title) {
        this(title, true);
    }

    /**
     * Creates a new application window.
     * 
     * @param title     window title
     * @param resizable if false, the window will lack or have a disabled maximize
     *                  button, and size changes are turned off, i.e. the pointer
     *                  will neither show double arrows over the border nor resize
     *                  the window if dragged there
     */
    public Form(String title, boolean resizable) {
        assert title != null : "'title' must not be null";

        main = GUI.bind();
        Display gui = GUI.get();
        shell = new Shell(gui, resizable ? SHELL_TRIM : TITLE | MIN | CLOSE);
        shell.setText(title);
    }

    /**
     * Sets a flag such that the application exits as soon as possible. The event
     * loop of the GUI thread will check the status and exit if necessary.
     * 
     * @param status status code &gt;0 to return
     */
    public void die(int status) {
        assert status != 0 : "'status' must not be 0";
        exitStatus = status;
    }

    /**
     * Sets the window's inner size. That is the size available for rendering
     * elements.
     * 
     * @param width  width to set
     * @param height height to set
     */
    public void setInnerSize(int width, int height) {
        setSize(width + SWALLOWED_WIDTH, height + SWALLOWED_HEIGHT);
    }

    /**
     * Sets the window's (outer) size.
     * 
     * @param width  width to set
     * @param height height to set
     */
    public void setSize(int width, int height) {
        shell.setSize(width, height);
    }

    /**
     * Displays the window and leaves the choice which window of the application to interact with to
     * the user. The procedure moves the window to the top of the drawing order of
     * the GUI, sets it visible and asks the window manager to make the window
     * active. The user can still interact with other windows of the application.
     */
    public void show() {
        shell.open();
    }

    /**
     * &#x1F511; Displays the window and blocks interaction by the user to the
     * window that called this method. The procedure moves the window to the top of
     * the drawing order of the GUI, sets it visible and asks the window manager to
     * make the window active. The window will keep and nurse the calling thread
     * such that the user cannot interact with the calling window until this window
     * is closed and the thread is returned.
     */
    public void showModal() {
        try {
            show();
            Display gui = GUI.get();
            while (exitStatus == 0 && !shell.isDisposed()) {
                try {
                    if (!gui.readAndDispatch()) {
                        Runnable updateTask;
                        MessagePacket messagePacket;
                        if ((updateTask = updateTasksDeque.poll()) != null) {
                            updateTask.run();
                        } else if ((messagePacket = messagesDeque.poll()) != null) {
                            showDialog(messagePacket);
                        } else {
                            gui.sleep();
                        }
                    }
                } catch (RuntimeException errorBarrier) {
                    handleError(errorBarrier, CANCEL);
                }
            }
        } catch (RuntimeException errorBarrier) {
            int buttonClicked = handleError(errorBarrier, ABORT);
            if (main) exitStatus = buttonClicked;
        }
        if (main) {
            GUI.free();
            System.exit(exitStatus);
        }
    }

    private int handleError(Exception failure, int button) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        failure.printStackTrace(printWriter);
        String stackTrace = stringWriter.toString();
        String noTabsNorPackages = stackTrace.replaceAll("\t|(?<= ).*?/", "");
        String fileRemoved = noTabsNorPackages.replaceAll("(\\.([^\\.]+)\\.[^\\.]+\\()\\2.java:", "$1");
        String nonBreaking = fileRemoved.replaceAll(" +", "\u00A0");
    
        MessageBox dialog = new MessageBox(shell, ICON_ERROR | button);
        dialog.setText(failure.getClass().getSimpleName());
        String message = failure.getMessage();
        dialog.setMessage(message == null ? nonBreaking : message + lineSeparator()
                + lineSeparator() + nonBreaking);
        return dialog.open();
    }

    /**
     * Displays a standard information pop-up window from the GUI. The dialog should
     * blocks interaction with the underlying window while it is showing.
     * 
     * @param messagePacket a package containing the dialog caption, message, icon,
     *                    buttons, and optionally a status value if the program
     *                    should be terminated after the message was shown
     */
    public void showDialog(MessagePacket messagePacket) {
        if (GUI.currentThreadIsGuiThread()) {
            MessageBox dialog = new MessageBox(shell, messagePacket.getIcon() | messagePacket.getButtons());
            dialog.setText(messagePacket.getCaption());
            dialog.setMessage(messagePacket.getMessage());
            dialog.open();
        } else {
            messagesDeque.add(messagePacket);
        }
    }

    /**
     * Displays a standard failure pop-up window from the GUI. The design is left to
     * the GUI, but it might be a dialog box with the heading "Error", an X in a
     * circle, and a single button to confirm. The GUI may play a sound typically
     * associated with this type of information window at the moment it pops up. The
     * dialog box should blocks interaction with the underlying window while it is
     * showing.
     * 
     * The procedure is protected because it should only be executed by the GUI
     * thread to prevent the stacking of message dialog boxes.
     * 
     * @param caption message box caption
     * @param message message to display
     */
    public void showFail(String caption, String message) {
        showDialog(new MessagePacket(caption, message, ICON_ERROR, ABORT));
    }

    /**
     * Displays a standard failure pop-up window from the GUI, and exits the program
     * afterwards. The dialog windows' design is left to the GUI, but it might be a
     * dialog box with an X in a circle, and a single button to confirm. The GUI may
     * play a sound typically associated with this type of information window at the
     * moment it pops up. The dialog box should blocks interaction with the
     * underlying window while it is showing. After the user cliced the button, the
     * program will terminate.
     * 
     * @param caption message box caption
     * @param message message to display
     * @param status  status code to return by {@linkplain System#exit(int)}
     */
    public void showFailThenDie(String caption, String message, int status) {
        showDialog(new MessagePacket(caption, message, ICON_ERROR, ABORT, status));
    }

    /**
     * Displays a standard information pop-up window from the GUI. The design is
     * left to the GUI, but it might be a dialog box with the heading "Information,"
     * an "i" in a circle, and a single "OK" button. The GUI may play a sound
     * typically associated with this type of information window at the moment it
     * pops up. The dialog box should blocks interaction with the underlying window
     * while it is showing.
     * 
     * @param caption message box caption
     * @param message message to display
     */
    public void showInfo(String caption, String message) {
        showDialog(new MessagePacket(caption, message, ICON_INFORMATION, OK));
    }

    /**
     * Displays a standard warning pop-up window from the GUI. The design is left to
     * the GUI, but it might be a dialog box with the heading "Waring", an
     * exclamation mark in an upwards-pointing triangle, and a single button to
     * confirm. The GUI may play a sound typically associated with this type of
     * information window at the moment it pops up. The dialog box should blocks
     * interaction with the underlying window while it is showing.
     * 
     * @param caption message box caption
     * @param message message to display
     */
    public void showWarning(String caption, String message) {
        showDialog(new MessagePacket(caption, message, ICON_WARNING, YES));
    }

    void submitUpdateTask(Runnable updateTask) {
        updateTasksDeque.addLast(updateTask);
    }
}
