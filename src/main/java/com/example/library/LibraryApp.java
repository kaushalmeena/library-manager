package com.example.library;

import com.example.library.config.AppConfig;
import com.example.library.db.DataAccessException;
import com.example.library.ui.LoginFrame;
import com.example.library.ui.theme.AppIcon;
import com.example.library.ui.theme.Theme;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application entry point.
 *
 * <p>Opens the database in the user's data directory, creating and seeding it on first run, then
 * shows the sign-in window. Startup work happens before any window appears so a database problem
 * is reported plainly instead of surfacing as a half-drawn interface.
 */
public final class LibraryApp {

    private static final Logger LOG = Logger.getLogger(LibraryApp.class.getName());

    private LibraryApp() {
    }

    public static void main(String[] args) {
        // A crash on the event thread would otherwise vanish into the console unnoticed.
        Thread.setDefaultUncaughtExceptionHandler(LibraryApp::reportUnexpectedFailure);

        AppConfig config = AppConfig.fromSystemProperties();
        LibraryServices services;
        try {
            services = LibraryServices.create(config);
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "The database could not be opened", e);
            showStartupFailure(config, e);
            return;
        }

        boolean firstRun = services.wasSeeded();
        LOG.info(() -> "Using database " + config.databaseFile());

        Runtime.getRuntime().addShutdownHook(new Thread(services::close, "database-close"));

        SwingUtilities.invokeLater(() -> {
            Theme.install();
            AppIcon.applyToTaskbar();
            LoginFrame login = new LoginFrame(services);
            if (firstRun) {
                // A fresh database only holds demo accounts, so point the reader at one.
                login.prefill("admin", "password123");
            }
            login.setVisible(true);
            login.focusFirstField();
        });
    }

    private static void reportUnexpectedFailure(Thread thread, Throwable error) {
        LOG.log(Level.SEVERE, "Unexpected failure on " + thread.getName(), error);
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                "Something went wrong and the action could not be completed.\n\n"
                        + (error.getMessage() == null ? error.toString() : error.getMessage()),
                "Unexpected problem", JOptionPane.ERROR_MESSAGE));
    }

    private static void showStartupFailure(AppConfig config, Throwable error) {
        JOptionPane.showMessageDialog(null,
                "Library Manager could not open its database.\n\n"
                        + "Location: " + config.databaseFile() + "\n"
                        + "Reason: " + error.getMessage(),
                "Cannot start", JOptionPane.ERROR_MESSAGE);
    }
}
