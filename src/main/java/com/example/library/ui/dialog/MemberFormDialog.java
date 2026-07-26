package com.example.library.ui.dialog;

import com.example.library.LibraryServices;
import com.example.library.model.Role;
import com.example.library.model.User;
import com.example.library.service.ValidationException;
import com.example.library.ui.component.Card;
import com.example.library.ui.support.Dialogs;
import com.example.library.ui.support.FormBuilder;
import com.example.library.ui.theme.Theme;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;

/**
 * Creates or edits an account from the Members screen, where an admin can also choose the role.
 *
 * <p>When editing, the password fields are left blank and only change the password if filled in,
 * so an admin can correct a name without resetting anyone's credentials.
 */
public final class MemberFormDialog extends JDialog {

    private final LibraryServices services;
    private final User existing;

    private final JTextField nameField = new JTextField(18);
    private final JTextField emailField = new JTextField(18);
    private final JTextField mobileField = new JTextField(18);
    private final JTextField usernameField = new JTextField(18);
    private final JComboBox<Role> rolePicker = new JComboBox<>(Role.values());
    private final JPasswordField passwordField = new JPasswordField(18);

    private boolean saved;

    private MemberFormDialog(Window owner, LibraryServices services, User existing) {
        super(owner, existing == null ? "Add a member" : "Edit member",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        this.services = services;
        this.existing = existing;

        setContentPane(buildContent());
        Dialogs.closeOnEscape(this);
        if (existing != null) {
            populate(existing);
        }
        pack();
        setMinimumSize(new Dimension(560, getHeight()));
        setLocationRelativeTo(owner);
    }

    /**
     * Opens the dialog to add an account.
     *
     * @return {@code true} when an account was created
     */
    public static boolean showForNew(Window owner, LibraryServices services) {
        MemberFormDialog dialog = new MemberFormDialog(owner, services, null);
        dialog.setVisible(true);
        return dialog.saved;
    }

    /**
     * Opens the dialog to edit an account.
     *
     * @return {@code true} when the account was changed
     */
    public static boolean showForEdit(Window owner, LibraryServices services, User user) {
        MemberFormDialog dialog = new MemberFormDialog(owner, services, user);
        dialog.setVisible(true);
        return dialog.saved;
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(0, Theme.SPACE_4));
        root.setBorder(Theme.padding(Theme.SPACE_5));
        root.setBackground(Theme.canvas());

        JLabel heading = new JLabel(existing == null ? "Add a member" : "Edit member");
        heading.setFont(Theme.titleFont());
        heading.setForeground(Theme.textPrimary());

        rolePicker.setFont(Theme.bodyFont());
        rolePicker.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list,
                    Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Role role) {
                    setText(role.displayName());
                }
                return this;
            }
        });
        passwordField.putClientProperty("JPasswordField.showRevealButton", true);

        String passwordHelp = existing == null
                ? "At least 8 characters, with a letter and a digit"
                : "Leave blank to keep the current password";

        JPanel form = new FormBuilder(2)
                .add("Full name", nameField, null, 2)
                .add("Email", emailField)
                .add("Mobile", mobileField, "Optional")
                .add("Username", usernameField, "3-20 characters")
                .add("Role", rolePicker, "Decides which screens they can reach")
                .newRow()
                .add(existing == null ? "Password" : "New password", passwordField,
                        passwordHelp, 2)
                .build();

        Card body = new Card();
        body.add(form, BorderLayout.CENTER);

        JButton save = new JButton(existing == null ? "Create member" : "Save changes");
        save.setFont(Theme.bodyBoldFont());
        Theme.asPrimaryButton(save);
        save.addActionListener(e -> onSave());

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_2, 0));
        actions.setOpaque(false);
        actions.add(cancel);
        actions.add(save);

        root.add(heading, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(save);
        return root;
    }

    private void populate(User user) {
        nameField.setText(user.name());
        emailField.setText(user.email());
        mobileField.setText(user.mobile() == null ? "" : user.mobile());
        usernameField.setText(user.username());
        rolePicker.setSelectedItem(user.role());
    }

    private void onSave() {
        char[] password = passwordField.getPassword();
        try {
            Role role = (Role) rolePicker.getSelectedItem();
            if (existing == null) {
                User created = services.authService().register(nameField.getText(),
                        emailField.getText(), mobileField.getText(), usernameField.getText(),
                        password, role);
                saved = true;
                Dialogs.showSuccess(this, created.name() + " has been added as a "
                        + created.role().displayName().toLowerCase(java.util.Locale.ROOT) + ".");
            } else {
                services.authService().updateProfile(existing.id(), nameField.getText(),
                        emailField.getText(), mobileField.getText(), usernameField.getText(),
                        role);
                if (password.length > 0) {
                    services.authService().resetPassword(existing.id(), password);
                }
                saved = true;
                Dialogs.showSuccess(this, "The account has been updated.");
            }
            dispose();
        } catch (ValidationException e) {
            Dialogs.showValidationProblems(this, "This account could not be saved.",
                    e.problems());
        } catch (RuntimeException e) {
            Dialogs.showError(this, "This account could not be saved.", e);
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }
}
