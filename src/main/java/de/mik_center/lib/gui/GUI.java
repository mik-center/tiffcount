// Licensed under the Apache License, Version 2.0
// See http://www.apache.org/licenses/LICENSE-2.0

package de.mik_center.lib.gui;

import java.util.concurrent.*;

import org.eclipse.swt.widgets.*;

/**
 * Holds the JVM-wide GUI handler.
 */
public final class GUI {
    private static final CompletableFuture<Display> gui = new CompletableFuture<>();
    private static final CompletableFuture<Thread> guiThread = new CompletableFuture<>();

    /**
     * Binds with the GUI.
     * @return whether the window calling {@link #bind()} is the application's main
     *         window
     */
    public static boolean bind() {
        boolean firstInvocation = !gui.isDone();
        if (firstInvocation) {
            gui.complete(new Display());
            guiThread.complete(Thread.currentThread());
        }
        return firstInvocation;
    }

    /**
     * Removes non-Java content from memory.<!-- --> <B>Important!</B> The
     * underlying Standard Windowing Toolkit (SWT) framework uses binary code that
     * <B>must</B> be freed from the computer’s memory <I>manually</I> before the
     * application exits, or it will remain in memory <I>forever</I>, <B>creating a
     * memory leak even after the Java program was terminated!</B>
     * 
     * <P>
     * This procedure <B>must</B> be called from the same thread that called
     * {@linkplain #bind()}, you <B>can not</B> use
     * {@code Runtime.addShutdownHook()} to execute it. Make sure
     * {@code disconnect()} is also called in case of exceptions!
     */
    public static void free() {
        get().dispose();
    }

    /**
     * Returns the GUI windowHandle.
     * 
     * @return the GUI windowHandle
     */
    public static Display get() {
        Display gui = GUI.gui.getNow(null);
        assert gui != null : "GUI.connect() must have been called prior to calling get()";
        return gui;
    }

    public static boolean currentThreadIsGuiThread() {
        Thread guiThread = GUI.guiThread.getNow(null);
        assert guiThread != null
                : "GUI.connect() must have been called prior to calling currentThreadIsGuiThread()";
        return Thread.currentThread() == guiThread;
    }
}
