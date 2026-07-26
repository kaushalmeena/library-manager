package com.example.library.ui.view;

import com.example.library.LibraryServices;
import com.example.library.model.User;
import com.example.library.service.StatsService;
import com.example.library.service.ValidationException;
import com.example.library.ui.component.Badge;
import com.example.library.ui.component.Card;
import com.example.library.ui.support.Dialogs;
import com.example.library.ui.support.FormBuilder;
import com.example.library.ui.support.Formats;
import com.example.library.ui.theme.Theme;
import com.example.library.ui.theme.VectorIcon;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.function.Consumer;

/**
 * The signed-in person's own account: their details, their borrowing summary, and a password
 * change. Editing here never touches the role, which only an admin can change.
 */
public final class ProfileView extends View {

    private final LibraryServices services;
    private final Consumer<User> onAccountUpdated;

    private final JTextField nameField = new JTextField(18);
    private final JTextField emailField = new JTextField(18);
    private final JTextField mobileField = new JTextField(18);
    private final JTextField usernameField = new JTextField(18);

    private final JPasswordField currentPassword = new JPasswordField(18);
    private final JPasswordField newPassword = new JPasswordField(18);
    private final JPasswordField confirmPassword = new JPasswordField(18);

    private final JLabel heldLabel = new JLabel();
    private final JLabel historyLabel = new JLabel();
    private final JLabel finesLabel = new JLabel();
    private final JLabel memberSinceLabel = new JLabel();

    private User account;

    public ProfileView(LibraryServices services, User account, Consumer<User> onAccountUpdated) {
        super("Profile", "Your account details");
        this.services = services;
        this.account = account;
        this.onAccountUpdated = onAccountUpdated;

        setBody(buildBody());
    }

    private JPanel buildBody() {
        JPanel columns = new JPanel(new GridLayout(1, 2, Theme.SPACE_4, 0));
        columns.setOpaque(false);
        columns.add(buildLeftColumn());
        columns.add(buildRightColumn());
        return columns;
    }

    private JPanel buildLeftColumn() {
        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));

        JPanel detailsForm = new FormBuilder(2)
                .add("Full name", nameField, null, 2)
                .add("Email", emailField)
                .add("Mobile", mobileField, "Optional")
                .add("Username", usernameField, "3-20 characters", 2)
                .build();

        JButton saveDetails = new JButton("Save details");
        saveDetails.setFont(Theme.bodyBoldFont());
        Theme.asPrimaryButton(saveDetails);
        saveDetails.addActionListener(e -> saveDetails());

        JPanel detailsBody = new JPanel(new BorderLayout(0, Theme.SPACE_3));
        detailsBody.setOpaque(false);
        detailsBody.add(detailsForm, BorderLayout.CENTER);
        detailsBody.add(rightAligned(saveDetails), BorderLayout.SOUTH);

        currentPassword.putClientProperty("JPasswordField.showRevealButton", true);
        newPassword.putClientProperty("JPasswordField.showRevealButton", true);

        JPanel passwordForm = new FormBuilder(1)
                .add("Current password", currentPassword)
                .add("New password", newPassword,
                        "At least 8 characters, with a letter and a digit")
                .add("Confirm new password", confirmPassword)
                .build();

        JButton changePassword = new JButton("Change password");
        changePassword.setFont(Theme.bodyBoldFont());
        changePassword.addActionListener(e -> changePassword());

        JPanel passwordBody = new JPanel(new BorderLayout(0, Theme.SPACE_3));
        passwordBody.setOpaque(false);
        passwordBody.add(passwordForm, BorderLayout.CENTER);
        passwordBody.add(rightAligned(changePassword), BorderLayout.SOUTH);

        Card details = Card.titled("Your details", detailsBody);
        Card password = Card.titled("Password", passwordBody);
        details.setAlignmentX(LEFT_ALIGNMENT);
        password.setAlignmentX(LEFT_ALIGNMENT);

        stack.add(details);
        stack.add(Box.createVerticalStrut(Theme.SPACE_4));
        stack.add(password);
        stack.add(Box.createVerticalGlue());
        return stack;
    }

    private JPanel buildRightColumn() {
        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));

        JPanel identity = new JPanel(new BorderLayout(Theme.SPACE_3, 0));
        identity.setOpaque(false);
        JLabel icon = new JLabel(VectorIcon.of(VectorIcon.Glyph.USER_CHECK, 36, Theme.accent()));
        JPanel identityText = new JPanel();
        identityText.setOpaque(false);
        identityText.setLayout(new BoxLayout(identityText, BoxLayout.Y_AXIS));

        JLabel nameLabel = new JLabel(account.name());
        nameLabel.setFont(Theme.headingFont().deriveFont(17f));
        nameLabel.setForeground(Theme.textPrimary());
        Badge roleBadge = new Badge(account.role().displayName(), Theme.accent());
        nameLabel.setAlignmentX(LEFT_ALIGNMENT);
        roleBadge.setAlignmentX(LEFT_ALIGNMENT);
        identityText.add(nameLabel);
        identityText.add(Box.createVerticalStrut(Theme.SPACE_1));
        identityText.add(roleBadge);
        identity.add(icon, BorderLayout.WEST);
        identity.add(identityText, BorderLayout.CENTER);
        // Without a height cap the surrounding BoxLayout stretches this block and it swallows all
        // the spare space in the card, pushing the rows down and mis-centring the icon.
        identity.setAlignmentX(LEFT_ALIGNMENT);
        identity.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, identity.getPreferredSize().height));

        JPanel summary = new JPanel();
        summary.setOpaque(false);
        summary.setLayout(new BoxLayout(summary, BoxLayout.Y_AXIS));
        summary.add(identity);
        summary.add(Box.createVerticalStrut(Theme.SPACE_4));
        summary.add(summaryRow("Books held now", heldLabel));
        summary.add(summaryRow("Borrowed all time", historyLabel));
        summary.add(summaryRow("Fines owed", finesLabel));
        summary.add(summaryRow("Member since", memberSinceLabel));
        // Anything left over collects at the bottom instead of being shared between the rows.
        summary.add(Box.createVerticalGlue());

        Card summaryCard = Card.titled("At a glance", summary);

        JPanel policyText = new JPanel();
        policyText.setOpaque(false);
        policyText.setLayout(new BoxLayout(policyText, BoxLayout.Y_AXIS));
        policyText.add(policyRow("Loan period",
                services.config().loanDays() + " days"));
        policyText.add(policyRow("Borrowing limit",
                services.config().maxLoansPerMember() + " books at a time"));
        policyText.add(policyRow("Renewals allowed",
                services.config().maxRenewals() + " per loan"));
        policyText.add(policyRow("Late fine",
                services.config().money(services.config().finePerDay()) + " per day"));
        policyText.add(Box.createVerticalGlue());
        Card policyCard = Card.titled("Library policy", policyText);

        summaryCard.setAlignmentX(LEFT_ALIGNMENT);
        policyCard.setAlignmentX(LEFT_ALIGNMENT);
        stack.add(summaryCard);
        stack.add(Box.createVerticalStrut(Theme.SPACE_4));
        stack.add(policyCard);
        stack.add(Box.createVerticalGlue());
        return stack;
    }

    private JPanel summaryRow(String label, JLabel valueLabel) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(Theme.padding(Theme.SPACE_2, 0));

        JLabel key = new JLabel(label);
        key.setFont(Theme.bodyFont());
        key.setForeground(Theme.textSecondary());

        valueLabel.setFont(Theme.bodyBoldFont());
        valueLabel.setForeground(Theme.textPrimary());
        valueLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);

        row.add(key, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        row.setAlignmentX(LEFT_ALIGNMENT);
        return row;
    }

    private JPanel policyRow(String label, String value) {
        JLabel valueLabel = new JLabel(value);
        return summaryRow(label, valueLabel);
    }

    private static JPanel rightAligned(JButton button) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        panel.setOpaque(false);
        panel.add(button);
        return panel;
    }

    @Override
    public void refresh() {
        services.userRepository().findById(account.id()).ifPresent(latest -> account = latest);

        nameField.setText(account.name());
        emailField.setText(account.email());
        mobileField.setText(account.mobile() == null ? "" : account.mobile());
        usernameField.setText(account.username());

        StatsService.MemberStats stats = services.statsService().memberStats(account.id());
        heldLabel.setText(Formats.plural(stats.currentlyHeld(), "book", "books")
                + (stats.overdue() > 0 ? " (" + stats.overdue() + " overdue)" : ""));
        historyLabel.setText(Formats.plural(stats.borrowedEverTotal(), "book", "books"));
        finesLabel.setText(services.config().money(stats.finesOwed()));
        finesLabel.setForeground(stats.finesOwed().signum() > 0
                ? Theme.danger()
                : Theme.textPrimary());
        memberSinceLabel.setText(Formats.date(account.createdDate()));

        setSubtitle("Signed in as " + account.username() + " · "
                + account.role().displayName());
    }

    private void saveDetails() {
        try {
            User updated = services.authService().updateProfile(account.id(), nameField.getText(),
                    emailField.getText(), mobileField.getText(), usernameField.getText(),
                    account.role());
            account = updated;
            Dialogs.showSuccess(this, "Your details have been saved.");
            onAccountUpdated.accept(updated);
        } catch (ValidationException e) {
            Dialogs.showValidationProblems(this, "Your details could not be saved.",
                    e.problems());
        } catch (RuntimeException e) {
            Dialogs.showError(this, "Your details could not be saved.", e);
        }
    }

    private void changePassword() {
        char[] current = currentPassword.getPassword();
        char[] fresh = newPassword.getPassword();
        char[] confirmation = confirmPassword.getPassword();
        try {
            services.authService().changePassword(account, current, fresh, confirmation);
            currentPassword.setText("");
            newPassword.setText("");
            confirmPassword.setText("");
            Dialogs.showSuccess(this, "Your password has been changed.");
        } catch (ValidationException e) {
            Dialogs.showValidationProblems(this, "Your password was not changed.", e.problems());
        } catch (RuntimeException e) {
            Dialogs.showError(this, "Your password was not changed.", e);
        } finally {
            java.util.Arrays.fill(current, '\0');
            java.util.Arrays.fill(fresh, '\0');
            java.util.Arrays.fill(confirmation, '\0');
        }
    }
}
