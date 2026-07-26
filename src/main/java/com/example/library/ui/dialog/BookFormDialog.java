package com.example.library.ui.dialog;

import com.example.library.LibraryServices;
import com.example.library.model.Book;
import com.example.library.model.BookMetadata;
import com.example.library.service.CatalogueService;
import com.example.library.service.MetadataService;
import com.example.library.service.ValidationException;
import com.example.library.ui.component.Card;
import com.example.library.ui.component.CoverArt;
import com.example.library.ui.support.Async;
import com.example.library.ui.support.Dialogs;
import com.example.library.ui.support.FormBuilder;
import com.example.library.ui.theme.Theme;
import com.example.library.ui.theme.VectorIcon;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Creates or edits a catalogue entry.
 *
 * <p>The ISBN field has a Look up button that queries Open Library and fills in the title,
 * author, publisher, year and cover art. That request runs on a background thread, so a slow
 * network cannot freeze the window, and the cover appears beside the form as soon as it lands.
 */
public final class BookFormDialog extends JDialog {

    private static final int COVER_WIDTH = 150;
    private static final int COVER_HEIGHT = 215;

    private final LibraryServices services;
    private final Book original;

    private final JTextField isbnField = new JTextField(16);
    private final JTextField titleField = new JTextField(24);
    private final JTextField authorField = new JTextField(18);
    private final JTextField publisherField = new JTextField(18);
    private final JTextField yearField = new JTextField(6);
    private final JTextField priceField = new JTextField(8);
    private final JSpinner copiesSpinner =
            new JSpinner(new SpinnerNumberModel(1, 0, 999, 1));
    private final CoverArt cover = new CoverArt(COVER_WIDTH, COVER_HEIGHT);
    private final JLabel lookupStatus = new JLabel(" ");
    private final JButton lookupButton;

    private String coverUrl;
    private boolean saved;
    private long savedId;

    private BookFormDialog(Window owner, LibraryServices services, Book existing) {
        super(owner, existing == null ? "Add a book" : "Edit book",
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        this.services = services;
        this.original = existing;

        lookupButton = new JButton("Look up",
                VectorIcon.of(VectorIcon.Glyph.SEARCH, 15, Theme.accent()));
        lookupButton.setFont(Theme.smallBoldFont());
        lookupButton.setToolTipText("Fetch the title, author, publisher and cover from "
                + "Open Library");
        lookupButton.addActionListener(e -> lookUpIsbn());

        setContentPane(buildContent());
        Dialogs.closeOnEscape(this);
        if (existing != null) {
            populate(existing);
        }
        pack();
        setMinimumSize(new Dimension(720, getHeight()));
        setLocationRelativeTo(owner);
    }

    /**
     * Opens the dialog to add a title.
     *
     * @return the new title's identifier, or empty when cancelled
     */
    public static Optional<Long> showForNew(Window owner, LibraryServices services) {
        return showAndCollect(new BookFormDialog(owner, services, null));
    }

    /**
     * Opens the dialog to edit an existing title.
     *
     * @return the saved title's identifier, or empty when cancelled
     */
    public static Optional<Long> showForEdit(Window owner, LibraryServices services, Book book) {
        return showAndCollect(new BookFormDialog(owner, services, book));
    }

    private static Optional<Long> showAndCollect(BookFormDialog dialog) {
        dialog.setVisible(true);
        return dialog.saved ? Optional.of(dialog.savedId) : Optional.empty();
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(0, Theme.SPACE_4));
        root.setBorder(Theme.padding(Theme.SPACE_5));
        root.setBackground(Theme.canvas());

        JLabel heading = new JLabel(original == null ? "Add a book" : "Edit book");
        heading.setFont(Theme.titleFont());
        heading.setForeground(Theme.textPrimary());

        JLabel subheading = new JLabel(
                "Enter an ISBN and press Look up to fill the details automatically.");
        subheading.setFont(Theme.bodyFont());
        subheading.setForeground(Theme.textSecondary());

        JPanel header = new JPanel(new BorderLayout(0, Theme.SPACE_1));
        header.setOpaque(false);
        header.add(heading, BorderLayout.NORTH);
        header.add(subheading, BorderLayout.SOUTH);

        JPanel isbnRow = new JPanel(new BorderLayout(Theme.SPACE_2, 0));
        isbnRow.setOpaque(false);
        isbnRow.add(isbnField, BorderLayout.CENTER);
        isbnRow.add(lookupButton, BorderLayout.EAST);

        lookupStatus.setFont(Theme.smallFont());
        lookupStatus.setForeground(Theme.textMuted());

        priceField.setToolTipText("Replacement price, used for the collection value report");

        JPanel form = new FormBuilder(2)
                .add("ISBN", isbnRow, "10 or 13 digits, separators are fine", 2)
                .addBare(lookupStatus, 2)
                .add("Title", titleField, null, 2)
                .add("Author", authorField)
                .add("Publisher", publisherField)
                .add("Published year", yearField, "Optional")
                .add("Price (" + services.config().currencySymbol() + ")", priceField)
                .add("Copies owned", copiesSpinner,
                        original == null ? null : "Cannot drop below the number on loan")
                .build();

        JLabel coverCaption = new JLabel("Cover", javax.swing.SwingConstants.CENTER);
        coverCaption.setFont(Theme.smallFont());
        coverCaption.setForeground(Theme.textMuted());
        coverCaption.setAlignmentX(javax.swing.JComponent.CENTER_ALIGNMENT);
        cover.setAlignmentX(javax.swing.JComponent.CENTER_ALIGNMENT);

        // A vertical stack keeps the caption directly under the artwork rather than letting the
        // layout centre it in the leftover space.
        JPanel coverStack = new JPanel();
        coverStack.setOpaque(false);
        coverStack.setLayout(new javax.swing.BoxLayout(coverStack, javax.swing.BoxLayout.Y_AXIS));
        coverStack.add(cover);
        coverStack.add(javax.swing.Box.createVerticalStrut(Theme.SPACE_2));
        coverStack.add(coverCaption);
        coverStack.add(javax.swing.Box.createVerticalGlue());

        JPanel coverPanel = new JPanel(new BorderLayout());
        coverPanel.setOpaque(false);
        coverPanel.add(coverStack, BorderLayout.NORTH);

        Card body = new Card(new BorderLayout(Theme.SPACE_5, 0));
        body.add(form, BorderLayout.CENTER);
        body.add(coverPanel, BorderLayout.EAST);

        JButton save = new JButton(original == null ? "Add to catalogue" : "Save changes");
        save.setFont(Theme.bodyBoldFont());
        Theme.asPrimaryButton(save);
        save.addActionListener(e -> onSave());

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_2, 0));
        actions.setOpaque(false);
        actions.add(cancel);
        actions.add(save);

        root.add(header, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(save);
        return root;
    }

    private void populate(Book book) {
        isbnField.setText(book.isbn() == null ? "" : book.isbn());
        titleField.setText(book.title());
        authorField.setText(book.author() == null ? "" : book.author());
        publisherField.setText(book.publisher() == null ? "" : book.publisher());
        yearField.setText(book.publishedYear() == null ? "" : book.publishedYear().toString());
        priceField.setText(book.price().toPlainString());
        copiesSpinner.setValue(book.totalCopies());
        coverUrl = book.coverUrl();
        cover.setCoverUrl(coverUrl);
    }

    /** Runs the Open Library lookup off the event thread and fills the form with the result. */
    private void lookUpIsbn() {
        String isbn = isbnField.getText();
        lookupButton.setEnabled(false);
        lookupStatus.setForeground(Theme.textMuted());
        lookupStatus.setText("Looking up " + isbn.trim() + " on Open Library…");

        Async.run(
                () -> services.metadataService().lookupByIsbn(isbn),
                found -> {
                    lookupButton.setEnabled(true);
                    if (found.isEmpty()) {
                        lookupStatus.setForeground(Theme.warning());
                        lookupStatus.setText("Open Library has no record of that ISBN. "
                                + "Fill the details in by hand.");
                        return;
                    }
                    applyMetadata(found.get());
                },
                error -> {
                    lookupButton.setEnabled(true);
                    lookupStatus.setForeground(Theme.danger());
                    lookupStatus.setText(error instanceof ValidationException
                            || error instanceof MetadataService.LookupException
                            ? error.getMessage()
                            : "The lookup failed. Fill the details in by hand.");
                });
    }

    /** Fills empty fields from a lookup, leaving anything already typed untouched. */
    private void applyMetadata(BookMetadata metadata) {
        isbnField.setText(metadata.isbn());
        if (metadata.title() != null) {
            titleField.setText(metadata.title());
        }
        if (metadata.author() != null && authorField.getText().isBlank()) {
            authorField.setText(metadata.author());
        }
        if (metadata.publisher() != null && publisherField.getText().isBlank()) {
            publisherField.setText(metadata.publisher());
        }
        if (metadata.publishedYear() != null && yearField.getText().isBlank()) {
            yearField.setText(metadata.publishedYear().toString());
        }
        if (metadata.coverUrl() != null) {
            coverUrl = metadata.coverUrl();
            cover.setCoverUrl(coverUrl);
        }
        lookupStatus.setForeground(Theme.success());
        lookupStatus.setText("Found \"" + metadata.title() + "\". Check the details and save.");
    }

    private void onSave() {
        try {
            long id = services.catalogueService().save(collectInput());
            savedId = id;
            saved = true;
            dispose();
        } catch (ValidationException e) {
            Dialogs.showValidationProblems(this, "This book could not be saved.", e.problems());
        } catch (RuntimeException e) {
            Dialogs.showError(this, "This book could not be saved.", e);
        }
    }

    /**
     * Reads the form into a {@link Book}.
     *
     * <p>Only the two numeric fields are checked here, because a typo like "twenty" has to be
     * reported before the value can reach the catalogue service at all. Every other rule lives
     * in {@link CatalogueService}.
     *
     * @throws ValidationException when the year or price is not a number
     */
    private Book collectInput() {
        java.util.List<String> problems = new java.util.ArrayList<>();
        Integer year = parseYear(yearField.getText(), problems);
        BigDecimal price = parsePrice(priceField.getText(), problems);
        if (!problems.isEmpty()) {
            throw new ValidationException(problems);
        }

        String isbn = CatalogueService.normaliseIsbn(isbnField.getText());
        // Offer the conventional cover for the ISBN when no lookup has been run.
        String resolvedCover = coverUrl != null ? coverUrl : MetadataService.coverUrlFor(isbn);

        return new Book(
                original == null ? 0 : original.id(),
                isbn,
                titleField.getText(),
                authorField.getText(),
                publisherField.getText(),
                year,
                resolvedCover,
                price,
                (Integer) copiesSpinner.getValue(),
                original == null ? LocalDate.now() : original.addedDate());
    }

    private static Integer parseYear(String text, java.util.List<String> problems) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException e) {
            problems.add("Published year must be a number, for example 2017.");
            return null;
        }
    }

    private static BigDecimal parsePrice(String text, java.util.List<String> problems) {
        if (text == null || text.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            problems.add("Price must be a number, for example 45.99.");
            return BigDecimal.ZERO;
        }
    }
}
