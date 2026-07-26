package com.example.library.ui.support;

import javax.swing.SwingWorker;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Runs work off the event dispatch thread and delivers the result back on it.
 *
 * <p>Swing requires that components are only touched from the EDT, and equally that the EDT is
 * never blocked. Anything that talks to the network, and every database read that feeds a
 * table, goes through here so a slow call cannot freeze the window.
 */
public final class Async {

    private Async() {
    }

    /**
     * Runs {@code work} in the background.
     *
     * @param work      the background computation
     * @param onSuccess receives the result on the EDT
     * @param onError   receives the failure on the EDT
     */
    public static <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.call();
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    onError.accept(cause);
                } catch (RuntimeException e) {
                    onError.accept(e);
                }
            }
        }.execute();
    }

    /**
     * Runs {@code work} in the background, reporting any failure through
     * {@link Dialogs#showError(java.awt.Component, String, Throwable)}.
     *
     * @param owner   the component the error dialog should be centred on
     * @param message what was being attempted, used as the error dialog's lead line
     */
    public static <T> void run(java.awt.Component owner, String message, Callable<T> work,
                               Consumer<T> onSuccess) {
        run(work, onSuccess, error -> Dialogs.showError(owner, message, error));
    }
}
