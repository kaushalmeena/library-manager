package com.example.library.ui.dialog;

import com.example.library.LibraryServices;
import com.example.library.model.Book;
import com.example.library.model.BookSummary;
import com.example.library.model.Loan;
import com.example.library.model.User;
import com.example.library.service.ValidationException;
import com.example.library.ui.component.Card;
import com.example.library.ui.component.CoverArt;
import com.example.library.ui.support.Dialogs;
import com.example.library.ui.support.Formats;
import com.example.library.ui.theme.Theme;
import com.example.library.ui.theme.VectorIcon;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.time.LocalDate;
import java.util.List;

/**
 * Issues a copy to a member.
 *
 * <p>Both the book and the member are chosen from pickers, and the dialog shows the due date
 * that will be applied plus any reason the member cannot borrow, so the librarian sees the
 * outcome before committing rather than after.
 */
public final class IssueDialog extends JDialog {

    private final LibraryServices services;
    private final JComboBox<BookSummary> bookPicker = new JComboBox<>();
    private final JComboBox<User> memberPicker = new JComboBox<>();
    private final CoverArt cover = new CoverArt(96, 138);
    private final JLabel dueDateLabel = new JLabel();
    private final JLabel eligibilityLabel = new JLabel(" ");
    private final JButton issueButton;

    private boolean issued;

    private IssueDialog(Window owner, LibraryServices services, Book preselected) {
        super(owner, "Issue a book", java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        this.services = services;

        issueButton = new JButton("Issue book");
        issueButton.setFont(Theme.bodyBoldFont());
        Theme.asPrimaryButton(issueButton);
        issueButton.addActionListener(e -> onIssue());

        setContentPane(buildContent());
        Dialogs.closeOnEscape(this);
        loadPickers(preselected);
        pack();
        setMinimumSize(new Dimension(560, getHeight()));
        setLocationRelativeTo(owner);
    }

    /**
     * Opens the dialog.
     *
     * @param preselected the book to start with, or {@code null} to choose one in the dialog
     * @return {@code true} when a loan was created
     */
    public static boolean show(Window owner, LibraryServices services, Book preselected) {
        IssueDialog dialog = new IssueDialog(owner, services, preselected);
        dialog.setVisible(true);
        return dialog.issued;
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(0, Theme.SPACE_4));
        root.setBorder(Theme.padding(Theme.SPACE_5));
        root.setBackground(Theme.canvas());

        JLabel heading = new JLabel("Issue a book",
                VectorIcon.of(VectorIcon.Glyph.CIRCULATION, 20, Theme.accent()),
                JLabel.LEFT);
        heading.setFont(Theme.titleFont());
        heading.setForeground(Theme.textPrimary());
        heading.setIconTextGap(Theme.SPACE_2);

        bookPicker.setFont(Theme.bodyFont());
        bookPicker.setRenderer(new BookRenderer());
        bookPicker.addActionListener(e -> onSelectionChanged());

        memberPicker.setFont(Theme.bodyFont());
        memberPicker.setRenderer(new MemberRenderer());
        memberPicker.addActionListener(e -> onSelectionChanged());

        dueDateLabel.setFont(Theme.bodyFont());
        dueDateLabel.setForeground(Theme.textSecondary());
        eligibilityLabel.setFont(Theme.smallFont());

        JPanel fields = new JPanel();
        fields.setOpaque(false);
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        fields.add(labelled("Book", bookPicker));
        fields.add(Box.createVerticalStrut(Theme.SPACE_3));
        fields.add(labelled("Member", memberPicker));
        fields.add(Box.createVerticalStrut(Theme.SPACE_3));
        fields.add(alignLeft(dueDateLabel));
        fields.add(Box.createVerticalStrut(Theme.SPACE_1));
        fields.add(alignLeft(eligibilityLabel));

        Card body = new Card(new BorderLayout(Theme.SPACE_4, 0));
        body.add(fields, BorderLayout.CENTER);
        body.add(cover, BorderLayout.EAST);

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_2, 0));
        actions.setOpaque(false);
        actions.add(cancel);
        actions.add(issueButton);

        root.add(heading, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(issueButton);
        return root;
    }

    private static JPanel labelled(String text, Component field) {
        JPanel panel = new JPanel(new BorderLayout(0, Theme.SPACE_1));
        panel.setOpaque(false);
        JLabel label = new JLabel(text);
        label.setFont(Theme.smallBoldFont());
        label.setForeground(Theme.textSecondary());
        panel.add(label, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
        return panel;
    }

    private static Component alignLeft(javax.swing.JComponent component) {
        component.setAlignmentX(LEFT_ALIGNMENT);
        return component;
    }

    private void loadPickers(Book preselected) {
        List<BookSummary> available = services.circulationService().availableBooks();
        available.forEach(bookPicker::addItem);
        if (preselected != null) {
            available.stream()
                    .filter(summary -> summary.book().id() == preselected.id())
                    .findFirst()
                    .ifPresent(bookPicker::setSelectedItem);
        }

        services.userRepository().findAll().forEach(memberPicker::addItem);

        if (available.isEmpty()) {
            eligibilityLabel.setForeground(Theme.danger());
            eligibilityLabel.setText("Every copy in the library is currently on loan.");
            issueButton.setEnabled(false);
        }
        onSelectionChanged();
    }

    /** Recomputes the due date and reports anything that would block the loan. */
    private void onSelectionChanged() {
        BookSummary book = (BookSummary) bookPicker.getSelectedItem();
        User member = (User) memberPicker.getSelectedItem();

        cover.setCoverUrl(book == null ? null : book.book().coverUrl());

        LocalDate due = services.circulationService().today()
                .plusDays(services.config().loanDays());
        dueDateLabel.setText("Due back on " + Formats.date(due) + " ("
                + services.config().loanDays() + " day loan)");

        if (book == null || member == null) {
            issueButton.setEnabled(false);
            return;
        }

        String blocker = findBlocker(book, member);
        if (blocker == null) {
            eligibilityLabel.setForeground(Theme.success());
            eligibilityLabel.setText(member.name() + " is clear to borrow this title.");
            issueButton.setEnabled(true);
        } else {
            eligibilityLabel.setForeground(Theme.danger());
            eligibilityLabel.setText(blocker);
            issueButton.setEnabled(false);
        }
    }

    /**
     * The first reason the loan would be refused, or {@code null} when it would go through.
     * The service enforces these rules too; this is purely so the librarian is told up front.
     */
    private String findBlocker(BookSummary book, User member) {
        if (!book.isAvailable()) {
            return "Every copy of this title is out.";
        }
        if (services.loanRepository().hasOpenLoan(book.book().id(), member.id())) {
            return member.name() + " is already holding a copy of this title.";
        }
        int held = services.loanRepository().countOutstandingForUser(member.id());
        if (held >= services.config().maxLoansPerMember()) {
            return member.name() + " already holds " + held + " books, the limit is "
                    + services.config().maxLoansPerMember() + ".";
        }
        int overdue = services.circulationService().countOverdueForUser(member.id());
        if (overdue > 0) {
            return member.name() + " has " + Formats.plural(overdue, "overdue book",
                    "overdue books") + " and cannot borrow until they are returned.";
        }
        return null;
    }

    private void onIssue() {
        BookSummary book = (BookSummary) bookPicker.getSelectedItem();
        User member = (User) memberPicker.getSelectedItem();
        if (book == null || member == null) {
            return;
        }
        try {
            Loan loan = services.circulationService().issue(book.book().id(), member.id());
            issued = true;
            Dialogs.showSuccess(this, "\"" + book.book().title() + "\" is issued to "
                    + member.name() + ", due back on " + Formats.date(loan.dueDate()) + ".");
            dispose();
        } catch (ValidationException e) {
            Dialogs.showValidationProblems(this, "That book could not be issued.", e.problems());
        } catch (RuntimeException e) {
            Dialogs.showError(this, "That book could not be issued.", e);
        }
    }

    /** Shows a title with its author and how many copies are free. */
    private static final class BookRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                                                      int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof BookSummary summary) {
                setText(summary.book().displayLabel() + "   ·   "
                        + summary.availabilityLabel() + " free");
            }
            return this;
        }
    }

    /** Shows a member with their role and email. */
    private static final class MemberRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                                                      int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof User user) {
                setText(user.name() + "   ·   " + user.role().displayName() + "   ·   "
                        + user.email());
            }
            return this;
        }
    }
}
