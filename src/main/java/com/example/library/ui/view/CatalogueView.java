package com.example.library.ui.view;

import com.example.library.LibraryServices;
import com.example.library.model.BookSummary;
import com.example.library.model.User;
import com.example.library.service.ValidationException;
import com.example.library.ui.component.Buttons;
import com.example.library.ui.component.Card;
import com.example.library.ui.component.SearchField;
import com.example.library.ui.dialog.BookFormDialog;
import com.example.library.ui.dialog.IssueDialog;
import com.example.library.ui.support.CsvExport;
import com.example.library.ui.support.Dialogs;
import com.example.library.ui.support.EntityTable;
import com.example.library.ui.support.Formats;
import com.example.library.ui.theme.Theme;
import com.example.library.ui.theme.VectorIcon;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * The catalogue: every title with its live availability, searchable, with the staff actions
 * operating on whatever row is selected.
 *
 * <p>Replacing the old "type a book id into a text field" flow with a selection means an
 * operation can no longer be aimed at the wrong record by a typo.
 */
public final class CatalogueView extends View {

    private final LibraryServices services;
    private final User account;
    private final Runnable onDataChanged;

    private final EntityTable<BookSummary> table;
    private final SearchField search;
    private final JCheckBox availableOnly;

    private JButton editButton;
    private JButton deleteButton;
    private JButton issueButton;

    public CatalogueView(LibraryServices services, User account, Runnable onDataChanged) {
        super("Catalogue", "Loading…");
        this.services = services;
        this.account = account;
        this.onDataChanged = onDataChanged;

        this.table = new EntityTable<>(columns());
        table.setEmptyMessage("The catalogue is empty. Add the first title to get started.");

        this.search = new SearchField("Search title, author, publisher or ISBN",
                table::setFilterText);
        this.availableOnly = new JCheckBox("Available only");
        availableOnly.setFont(Theme.bodyFont());
        availableOnly.setOpaque(false);
        availableOnly.addActionListener(e -> applyRowFilter());

        buildToolbar();
        setBody(buildBody());
        wireSelection();
    }

    private List<EntityTable.Column<BookSummary>> columns() {
        String currency = services.config().currencySymbol();
        return List.of(
                column("Title", 230, summary -> summary.book().title()).emphasised(),
                column("Author", 150, summary -> summary.book().author()),
                column("Publisher", 150, summary -> summary.book().publisher()),
                column("Year", 55, summary -> summary.book().publishedYear()).alignCenter(),
                column("ISBN", 120, summary -> summary.book().isbn()),
                column("Available", 90, BookSummary::available).alignCenter(),
                column("Copies", 65, summary -> summary.book().totalCopies()).alignCenter(),
                column("Times issued", 95, BookSummary::timesIssued).alignCenter(),
                column("Price", 80, summary -> summary.book().price(),
                        value -> Formats.moneyOrDash(currency, (java.math.BigDecimal) value))
                        .alignRight());
    }

    /** Typed shorthand so the column list above needs no explicit type arguments. */
    private static EntityTable.Column<BookSummary> column(String title, int width,
                                                          Function<BookSummary, Object> value) {
        return EntityTable.Column.of(title, width, value);
    }

    private static EntityTable.Column<BookSummary> column(String title, int width,
                                                          Function<BookSummary, Object> value,
                                                          Function<Object, String> display) {
        return EntityTable.Column.of(title, width, value, display);
    }

    private void buildToolbar() {
        if (account.role().canCirculate()) {
            issueButton = Buttons.primary("Issue this book", VectorIcon.Glyph.CIRCULATION);
            issueButton.setEnabled(false);
            issueButton.addActionListener(e -> issueSelected());
            addAction(issueButton);
        }

        if (account.role().canManageBooks()) {
            JButton addButton = Buttons.secondary("Add book", VectorIcon.Glyph.PLUS);
            addButton.addActionListener(e -> addBook());
            addAction(addButton);

            editButton = Buttons.secondary("Edit", VectorIcon.Glyph.EDIT);
            editButton.setEnabled(false);
            editButton.addActionListener(e -> editSelected());
            addAction(editButton);

            deleteButton = Buttons.danger("Remove", VectorIcon.Glyph.TRASH);
            deleteButton.setEnabled(false);
            deleteButton.addActionListener(e -> deleteSelected());
            addAction(deleteButton);
        }

        JButton exportButton = Buttons.secondary("Export CSV", VectorIcon.Glyph.DOWNLOAD);
        exportButton.addActionListener(e ->
                CsvExport.save(this, "catalogue", table.headers(), table.visibleRowsAsText()));
        addAction(exportButton);
    }

    private JPanel buildBody() {
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_3, 0));
        filters.setOpaque(false);
        filters.add(search);
        filters.add(availableOnly);

        Card card = new Card(new BorderLayout(0, Theme.SPACE_3));
        card.add(filters, BorderLayout.NORTH);
        card.add(table, BorderLayout.CENTER);
        return card;
    }

    private void wireSelection() {
        table.onSelectionChanged(selected -> {
            boolean present = selected.isPresent();
            if (editButton != null) {
                editButton.setEnabled(present);
            }
            if (deleteButton != null) {
                deleteButton.setEnabled(present);
            }
            if (issueButton != null) {
                issueButton.setEnabled(present && selected.get().isAvailable());
            }
        });
        if (account.role().canManageBooks()) {
            table.onRowActivated(summary -> editSelected());
        }
    }

    private void applyRowFilter() {
        table.setRowPredicate(availableOnly.isSelected() ? BookSummary::isAvailable : null);
    }

    @Override
    public void refresh() {
        List<BookSummary> summaries = services.catalogueService().allSummaries();
        table.setRows(summaries);
        applyRowFilter();

        int copies = summaries.stream().mapToInt(summary -> summary.book().totalCopies()).sum();
        int available = summaries.stream().mapToInt(BookSummary::available).sum();
        setSubtitle(Formats.plural(summaries.size(), "title", "titles") + " · " + copies
                + " copies · " + available + " on the shelf · collection worth "
                + services.config().money(services.bookRepository().collectionValue()));
    }

    private void addBook() {
        BookFormDialog.showForNew(owningWindow(), services).ifPresent(id -> {
            onDataChanged.run();
            selectById(id);
        });
    }

    private void editSelected() {
        table.selectedRow().ifPresent(summary ->
                BookFormDialog.showForEdit(owningWindow(), services, summary.book())
                        .ifPresent(id -> {
                            onDataChanged.run();
                            selectById(id);
                        }));
    }

    private void deleteSelected() {
        Optional<BookSummary> selected = table.selectedRow();
        if (selected.isEmpty()) {
            return;
        }
        BookSummary summary = selected.get();
        if (summary.onLoan() > 0) {
            Dialogs.showWarning(this, "Still on loan",
                    Formats.plural(summary.onLoan(), "copy", "copies")
                            + " of \"" + summary.book().title() + "\" "
                            + (summary.onLoan() == 1 ? "is" : "are")
                            + " still out. They have to come back before the title can be "
                            + "removed.");
            return;
        }
        boolean confirmed = Dialogs.confirmDestructive(this, "Remove title",
                "Remove \"" + summary.book().displayLabel() + "\" from the catalogue? "
                        + "Its borrowing history will be removed too.",
                "Remove");
        if (!confirmed) {
            return;
        }
        try {
            services.catalogueService().delete(summary.book().id());
            onDataChanged.run();
            Dialogs.showSuccess(this, "\"" + summary.book().title() + "\" has been removed.");
        } catch (ValidationException e) {
            Dialogs.showValidationProblems(this, "That title could not be removed.",
                    e.problems());
        } catch (RuntimeException e) {
            Dialogs.showError(this, "That title could not be removed.", e);
        }
    }

    private void issueSelected() {
        table.selectedRow().ifPresent(summary -> {
            if (IssueDialog.show(owningWindow(), services, summary.book())) {
                onDataChanged.run();
            }
        });
    }

    private void selectById(long bookId) {
        table.rows().stream()
                .filter(summary -> summary.book().id() == bookId)
                .findFirst()
                .ifPresent(table::selectRow);
    }

    private java.awt.Window owningWindow() {
        return javax.swing.SwingUtilities.getWindowAncestor(this);
    }
}
