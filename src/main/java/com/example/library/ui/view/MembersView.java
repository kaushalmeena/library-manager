package com.example.library.ui.view;

import com.example.library.LibraryServices;
import com.example.library.model.Role;
import com.example.library.model.User;
import com.example.library.service.ValidationException;
import com.example.library.ui.component.Buttons;
import com.example.library.ui.component.Card;
import com.example.library.ui.component.SearchField;
import com.example.library.ui.dialog.MemberFormDialog;
import com.example.library.ui.support.CsvExport;
import com.example.library.ui.support.Dialogs;
import com.example.library.ui.support.EntityTable;
import com.example.library.ui.support.Formats;
import com.example.library.ui.theme.Theme;
import com.example.library.ui.theme.VectorIcon;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Account management, reachable only by an admin. */
public final class MembersView extends View {

    /** A member with the circulation figures shown beside them. */
    private record MemberRow(User user, int held, int overdue, BigDecimal owed, int borrowedEver) {
    }

    private final LibraryServices services;
    private final User account;
    private final Runnable onDataChanged;

    private final EntityTable<MemberRow> table;
    private final SearchField search;

    private final JButton editButton;
    private final JButton deleteButton;

    public MembersView(LibraryServices services, User account, Runnable onDataChanged) {
        super("Members", "Loading…");
        this.services = services;
        this.account = account;
        this.onDataChanged = onDataChanged;

        this.table = new EntityTable<>(columns());
        table.setEmptyMessage("No accounts yet.");

        this.search = new SearchField("Search name, email, username or role",
                table::setFilterText);

        JButton addButton = Buttons.primary("Add member", VectorIcon.Glyph.PLUS);
        addButton.addActionListener(e -> addMember());
        addAction(addButton);

        editButton = Buttons.secondary("Edit", VectorIcon.Glyph.EDIT);
        editButton.setEnabled(false);
        editButton.addActionListener(e -> editSelected());
        addAction(editButton);

        deleteButton = Buttons.danger("Remove", VectorIcon.Glyph.TRASH);
        deleteButton.setEnabled(false);
        deleteButton.addActionListener(e -> deleteSelected());
        addAction(deleteButton);

        JButton exportButton = Buttons.secondary("Export CSV", VectorIcon.Glyph.DOWNLOAD);
        exportButton.addActionListener(e ->
                CsvExport.save(this, "members", table.headers(), table.visibleRowsAsText()));
        addAction(exportButton);

        setBody(buildBody());
        table.onSelectionChanged(this::updateActionState);
        table.onRowActivated(row -> editSelected());
    }

    private List<EntityTable.Column<MemberRow>> columns() {
        String currency = services.config().currencySymbol();
        // Widths are ratios: the table divides the space it has, so the total is kept modest
        // enough that no header has to be truncated.
        return List.of(
                column("Name", 150, row -> row.user().name()).emphasised(),
                column("Role", 95, row -> row.user().role()).alignCenter(),
                column("Email", 175, row -> row.user().email()),
                column("Username", 100, row -> row.user().username()),
                column("Mobile", 105, row -> row.user().mobile()),
                column("Held", 70, MemberRow::held).alignCenter(),
                column("Late", 70, MemberRow::overdue).alignCenter(),
                column("Loans", 75, MemberRow::borrowedEver).alignCenter(),
                column("Fines", 85, MemberRow::owed,
                        value -> Formats.moneyOrDash(currency, (BigDecimal) value)).alignRight(),
                column("Joined", 100, row -> row.user().createdDate(), Formats::dateCell));
    }

    /** Typed shorthand so the column list above needs no explicit type arguments. */
    private static EntityTable.Column<MemberRow> column(String title, int width,
                                                        Function<MemberRow, Object> value) {
        return EntityTable.Column.of(title, width, value);
    }

    private static EntityTable.Column<MemberRow> column(String title, int width,
                                                        Function<MemberRow, Object> value,
                                                        Function<Object, String> display) {
        return EntityTable.Column.of(title, width, value, display);
    }

    private JPanel buildBody() {
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_3, 0));
        filters.setOpaque(false);
        filters.add(search);

        Card card = new Card(new BorderLayout(0, Theme.SPACE_3));
        card.add(filters, BorderLayout.NORTH);
        card.add(table, BorderLayout.CENTER);
        return card;
    }

    private void updateActionState(Optional<MemberRow> selected) {
        editButton.setEnabled(selected.isPresent());
        // Nobody may delete their own account, so the button stays off for the current user.
        deleteButton.setEnabled(selected
                .map(row -> row.user().id() != account.id())
                .orElse(false));
    }

    @Override
    public void refresh() {
        List<User> users = services.userRepository().findAll();
        List<MemberRow> rows = users.stream().map(this::toRow).toList();
        table.setRows(rows);

        Map<Role, Long> byRole = users.stream()
                .collect(Collectors.groupingBy(User::role, Collectors.counting()));
        setSubtitle(Formats.plural(users.size(), "account", "accounts") + " · "
                + byRole.getOrDefault(Role.ADMIN, 0L) + " admin · "
                + byRole.getOrDefault(Role.LIBRARIAN, 0L) + " librarian · "
                + byRole.getOrDefault(Role.STUDENT, 0L) + " student");
        updateActionState(table.selectedRow());
    }

    private MemberRow toRow(User user) {
        var stats = services.statsService().memberStats(user.id());
        return new MemberRow(user, stats.currentlyHeld(), stats.overdue(), stats.finesOwed(),
                stats.borrowedEverTotal());
    }

    private void addMember() {
        if (MemberFormDialog.showForNew(owningWindow(), services)) {
            onDataChanged.run();
        }
    }

    private void editSelected() {
        table.selectedRow().ifPresent(row -> {
            if (MemberFormDialog.showForEdit(owningWindow(), services, row.user())) {
                onDataChanged.run();
            }
        });
    }

    private void deleteSelected() {
        Optional<MemberRow> selected = table.selectedRow();
        if (selected.isEmpty()) {
            return;
        }
        MemberRow row = selected.get();
        if (row.held() > 0) {
            Dialogs.showWarning(this, "Still holding books",
                    row.user().name() + " is holding "
                            + Formats.plural(row.held(), "book", "books")
                            + ". They have to be returned before the account can be removed.");
            return;
        }
        boolean confirmed = Dialogs.confirmDestructive(this, "Remove account",
                "Remove " + row.user().name() + "'s account? Their borrowing history will be "
                        + "removed with it.",
                "Remove");
        if (!confirmed) {
            return;
        }
        try {
            services.authService().deleteAccount(account, row.user().id());
            onDataChanged.run();
            Dialogs.showSuccess(this, row.user().name() + "'s account has been removed.");
        } catch (ValidationException e) {
            Dialogs.showValidationProblems(this, "That account could not be removed.",
                    e.problems());
        } catch (RuntimeException e) {
            Dialogs.showError(this, "That account could not be removed.", e);
        }
    }

    private java.awt.Window owningWindow() {
        return javax.swing.SwingUtilities.getWindowAncestor(this);
    }
}
