package com.example.library.ui.support;

import com.example.library.service.ValidationException;
import com.example.library.ui.theme.Theme;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.util.List;

/**
 * Consistent message, confirmation and error dialogs.
 *
 * <p>Messages are rendered as small HTML documents so long text wraps and lists of validation
 * problems appear as bullets instead of one run-on line.
 */
public final class Dialogs {

    private static final int WRAP_WIDTH_PX = 360;

    private Dialogs() {
    }

    /** An informational message. */
    public static void showInfo(Component owner, String title, String message) {
        JOptionPane.showMessageDialog(owner, wrap(message), title,
                JOptionPane.INFORMATION_MESSAGE);
    }

    /** A success message, worded and titled for a completed action. */
    public static void showSuccess(Component owner, String message) {
        showInfo(owner, "Done", message);
    }

    /** A warning that stops short of being an error. */
    public static void showWarning(Component owner, String title, String message) {
        JOptionPane.showMessageDialog(owner, wrap(message), title, JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Reports a failure.
     *
     * <p>A {@link ValidationException} is shown as the bulleted list of what is wrong, since
     * those messages are written for the person using the application. Anything else is
     * reported as an unexpected problem with its message attached.
     */
    public static void showError(Component owner, String lead, Throwable error) {
        if (error instanceof ValidationException validation) {
            showValidationProblems(owner, lead, validation.problems());
            return;
        }
        String detail = error == null || error.getMessage() == null
                ? "No further detail is available."
                : error.getMessage();
        JOptionPane.showMessageDialog(owner, wrap(lead + "<br><br>" + escape(detail)),
                "Something went wrong", JOptionPane.ERROR_MESSAGE);
    }

    /** Reports a plain error message with no exception behind it. */
    public static void showError(Component owner, String message) {
        JOptionPane.showMessageDialog(owner, wrap(message), "Something went wrong",
                JOptionPane.ERROR_MESSAGE);
    }

    /** Shows validation problems as a bulleted list under a lead line. */
    public static void showValidationProblems(Component owner, String lead, List<String> problems) {
        StringBuilder html = new StringBuilder("<html><body style='width:")
                .append(WRAP_WIDTH_PX).append("px'>")
                .append(escape(lead));
        if (problems.size() == 1) {
            html.append("<br><br>").append(escape(problems.get(0)));
        } else {
            html.append("<ul style='margin-left:14px'>");
            for (String problem : problems) {
                html.append("<li>").append(escape(problem)).append("</li>");
            }
            html.append("</ul>");
        }
        html.append("</body></html>");
        JOptionPane.showMessageDialog(owner, new JLabel(html.toString()),
                "Please check the details", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Asks a yes or no question.
     *
     * @return {@code true} when the person confirmed
     */
    public static boolean confirm(Component owner, String title, String message,
                                  String confirmLabel) {
        Object[] options = {confirmLabel, "Cancel"};
        int choice = JOptionPane.showOptionDialog(owner, wrap(message), title,
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options,
                options[0]);
        return choice == 0;
    }

    /**
     * Asks a yes or no question about something destructive, defaulting to Cancel.
     *
     * @return {@code true} when the person confirmed
     */
    public static boolean confirmDestructive(Component owner, String title, String message,
                                            String confirmLabel) {
        Object[] options = {confirmLabel, "Cancel"};
        int choice = JOptionPane.showOptionDialog(owner, wrap(message), title,
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options,
                options[1]);
        return choice == 0;
    }

    /**
     * Makes Escape cancel a dialog, which every dialog in the application is expected to do.
     *
     * @param dialog the dialog to close when Escape is pressed
     */
    public static void closeOnEscape(javax.swing.JDialog dialog) {
        javax.swing.JComponent root = dialog.getRootPane();
        javax.swing.KeyStroke escape =
                javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0);
        root.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(escape, "cancel");
        root.getActionMap().put("cancel", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                dialog.dispose();
            }
        });
    }

    /** Wraps a message in a fixed-width HTML label so long lines break sensibly. */
    private static JLabel wrap(String message) {
        JLabel label = new JLabel("<html><body style='width:" + WRAP_WIDTH_PX + "px'>"
                + (message.contains("<br") || message.contains("<ul")
                        ? message
                        : escape(message))
                + "</body></html>");
        label.setFont(Theme.bodyFont());
        return label;
    }

    /** Escapes the characters that would otherwise be read as HTML markup. */
    private static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");
    }
}
