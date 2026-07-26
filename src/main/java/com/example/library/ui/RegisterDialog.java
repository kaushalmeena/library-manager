package com.example.library.ui;

import com.example.library.LibraryServices;
import com.example.library.model.Role;
import com.example.library.model.User;
import com.example.library.service.ValidationException;
import com.example.library.ui.support.Dialogs;
import com.example.library.ui.support.FormBuilder;
import com.example.library.ui.theme.Theme;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.Optional;

/**
 * Self-service registration, reachable from the sign-in window. Accounts created here are always
 * students; staff accounts are created by an admin from the Members screen.
 */
public final class RegisterDialog extends JDialog {

    private final LibraryServices services;

    private final JTextField nameField = new JTextField(18);
    private final JTextField emailField = new JTextField(18);
    private final JTextField mobileField = new JTextField(18);
    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JPasswordField confirmField = new JPasswordField(18);

    private String registeredUsername;

    public RegisterDialog(Frame owner, LibraryServices services) {
        super(owner, "Create your account", true);
        this.services = services;

        setContentPane(buildContent());
        Dialogs.closeOnEscape(this);
        pack();
        setMinimumSize(new Dimension(520, getHeight()));
        setLocationRelativeTo(owner);
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(0, Theme.SPACE_4));
        root.setBorder(Theme.padding(Theme.SPACE_5));
        root.setBackground(Theme.canvas());

        JLabel title = new JLabel("Create your account");
        title.setFont(Theme.titleFont());
        title.setForeground(Theme.textPrimary());

        JLabel subtitle = new JLabel("You will be registered as a student member.");
        subtitle.setFont(Theme.bodyFont());
        subtitle.setForeground(Theme.textSecondary());

        JPanel header = new JPanel(new BorderLayout(0, Theme.SPACE_1));
        header.setOpaque(false);
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);

        passwordField.putClientProperty("JPasswordField.showRevealButton", true);

        JPanel form = new FormBuilder(2)
                .add("Full name", nameField, null, 2)
                .add("Email", emailField)
                .add("Mobile", mobileField, "Optional")
                .add("Username", usernameField, "3-20 characters")
                .newRow()
                .add("Password", passwordField, "At least 8 characters, with a letter and a digit")
                .add("Confirm password", confirmField)
                .build();

        JButton create = new JButton("Create account");
        create.setFont(Theme.bodyBoldFont());
        Theme.asPrimaryButton(create);
        create.addActionListener(e -> onSubmit());

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_2, 0));
        actions.setOpaque(false);
        actions.add(cancel);
        actions.add(create);

        root.add(header, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(create);
        return root;
    }

    private void onSubmit() {
        char[] password = passwordField.getPassword();
        char[] confirmation = confirmField.getPassword();
        try {
            if (!java.util.Arrays.equals(password, confirmation)) {
                Dialogs.showValidationProblems(this, "That account could not be created.",
                        java.util.List.of("Password and confirmation do not match."));
                return;
            }
            User created = services.authService().register(
                    nameField.getText(), emailField.getText(), mobileField.getText(),
                    usernameField.getText(), password, Role.STUDENT);
            registeredUsername = created.username();
            Dialogs.showSuccess(this, "Welcome, " + created.name()
                    + ". You can sign in with the username " + created.username() + ".");
            dispose();
        } catch (ValidationException e) {
            Dialogs.showValidationProblems(this, "That account could not be created.",
                    e.problems());
        } catch (RuntimeException e) {
            Dialogs.showError(this, "That account could not be created.", e);
        } finally {
            java.util.Arrays.fill(password, '\0');
            java.util.Arrays.fill(confirmation, '\0');
        }
    }

    /** The username that was created, or empty when the dialog was cancelled. */
    public Optional<String> registeredUsername() {
        return Optional.ofNullable(registeredUsername);
    }
}
