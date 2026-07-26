package com.example.library.ui.view;

import com.example.library.LibraryServices;
import com.example.library.model.LoanDetail;
import com.example.library.model.User;
import com.example.library.service.CirculationService;
import com.example.library.service.ValidationException;
import com.example.library.ui.component.Buttons;
import com.example.library.ui.component.Card;
import com.example.library.ui.component.SearchField;
import com.example.library.ui.dialog.IssueDialog;
import com.example.library.ui.support.CsvExport;
import com.example.library.ui.support.Dialogs;
import com.example.library.ui.support.EntityTable;
import com.example.library.ui.support.Formats;
import com.example.library.ui.theme.Theme;
import com.example.library.ui.theme.VectorIcon;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The circulation desk. Staff see every loan and can issue, return, renew and collect fines;
 * a student sees their own borrowing history and nothing else.
 *
 * <p>Because returns close a loan rather than delete it, this screen can show the full history
 * alongside what is currently out, which the previous version could not do at all.
 */
public final class CirculationView extends View {

    /** The segmented filter above the table. */
    private enum Scope {
        OUTSTANDING("Out now"),
        OVERDUE("Overdue"),
        RETURNED("Returned"),
        ALL("All");

        private final String label;

        Scope(String label) {
            this.label = label;
        }
    }

    private final LibraryServices services;
    private final User account;
    private final Runnable onDataChanged;
    private final boolean staffView;

    private final EntityTable<LoanDetail> table;
    private final SearchField search;

    private JButton returnButton;
    private JButton renewButton;
    private JButton payFineButton;

    private Scope scope = Scope.OUTSTANDING;

    public CirculationView(LibraryServices services, User account, Runnable onDataChanged) {
        super(account.role().canSeeAllLoans() ? "Circulation" : "My loans", "Loading…");
        this.services = services;
        this.account = account;
        this.onDataChanged = onDataChanged;
        this.staffView = account.role().canSeeAllLoans();

        this.table = new EntityTable<>(columns());
        table.setEmptyMessage(staffView
                ? "No loans recorded yet. Issue a book to get started."
                : "You have not borrowed anything yet. Browse the catalogue to find something.");
        table.sortBy(4, true);

        this.search = new SearchField(
                staffView ? "Search book, member or status" : "Search your loans",
                table::setFilterText);

        buildToolbar();
        setBody(buildBody());
        wireSelection();
    }

    private List<EntityTable.Column<LoanDetail>> columns() {
        String currency = services.config().currencySymbol();
        LocalDate today = services.circulationService().today();

        // Widths are ratios: the table divides the space it has, so the total is kept modest
        // enough that no header has to be truncated.
        if (staffView) {
            return List.of(
                    column("Book", 195, LoanDetail::bookTitle).emphasised(),
                    column("Member", 135, LoanDetail::memberName),
                    column("Status", 105, detail ->
                            detail.status(today, CirculationService.DUE_SOON_WINDOW_DAYS))
                            .alignCenter(),
                    column("Issued", 100, detail -> detail.loan().issueDate(), Formats::dateCell),
                    column("Due", 100, detail -> detail.loan().dueDate(), Formats::dateCell),
                    column("Returned", 100, detail -> detail.loan().returnDate(),
                            Formats::dateCell),
                    column("Renews", 75, detail -> detail.loan().renewals()).alignCenter(),
                    column("Fine", 78, LoanDetail::fine, money(currency)).alignRight(),
                    column("Owed", 78, LoanDetail::outstandingFine, money(currency))
                            .alignRight());
        }

        // A student already knows who they are, so the member column is dropped.
        return List.of(
                column("Book", 230, LoanDetail::bookTitle).emphasised(),
                column("Author", 160, LoanDetail::bookAuthor),
                column("Status", 110, detail ->
                        detail.status(today, CirculationService.DUE_SOON_WINDOW_DAYS))
                        .alignCenter(),
                column("Issued", 105, detail -> detail.loan().issueDate(), Formats::dateCell),
                column("Due", 105, detail -> detail.loan().dueDate(), Formats::dateCell),
                column("Returned", 105, detail -> detail.loan().returnDate(), Formats::dateCell),
                column("Fine", 80, LoanDetail::fine, money(currency)).alignRight(),
                column("Owed", 80, LoanDetail::outstandingFine, money(currency)).alignRight());
    }

    /** Typed shorthand so the column lists above need no explicit type arguments. */
    private static EntityTable.Column<LoanDetail> column(String title, int width,
                                                         Function<LoanDetail, Object> value) {
        return EntityTable.Column.of(title, width, value);
    }

    private static EntityTable.Column<LoanDetail> column(String title, int width,
                                                         Function<LoanDetail, Object> value,
                                                         Function<Object, String> display) {
        return EntityTable.Column.of(title, width, value, display);
    }

    private static Function<Object, String> money(String currency) {
        return value -> Formats.moneyOrDash(currency, (BigDecimal) value);
    }

    private void buildToolbar() {
        if (staffView) {
            JButton issueButton = Buttons.primary("Issue a book", VectorIcon.Glyph.PLUS);
            issueButton.addActionListener(e -> {
                if (IssueDialog.show(owningWindow(), services, null)) {
                    onDataChanged.run();
                }
            });
            addAction(issueButton);

            returnButton = Buttons.tinted("Return", VectorIcon.Glyph.CHECK, Theme.success());
            returnButton.setEnabled(false);
            returnButton.addActionListener(e -> returnSelected());
            addAction(returnButton);

            payFineButton = Buttons.tinted("Collect fine", VectorIcon.Glyph.COIN,
                    Theme.warning());
            payFineButton.setEnabled(false);
            payFineButton.addActionListener(e -> collectFine());
            addAction(payFineButton);
        }

        renewButton = Buttons.tinted("Renew", VectorIcon.Glyph.RENEW, Theme.info());
        renewButton.setEnabled(false);
        renewButton.addActionListener(e -> renewSelected());
        addAction(renewButton);

        JButton exportButton = Buttons.secondary("Export CSV", VectorIcon.Glyph.DOWNLOAD);
        exportButton.addActionListener(e ->
                CsvExport.save(this, "loans", table.headers(), table.visibleRowsAsText()));
        addAction(exportButton);
    }

    private JPanel buildBody() {
        JPanel toolbar = new JPanel(new BorderLayout(Theme.SPACE_4, 0));
        toolbar.setOpaque(false);
        toolbar.add(buildScopeSwitch(), BorderLayout.WEST);
        toolbar.add(search, BorderLayout.EAST);

        Card card = new Card(new BorderLayout(0, Theme.SPACE_3));
        card.add(toolbar, BorderLayout.NORTH);
        card.add(table, BorderLayout.CENTER);
        return card;
    }

    /** A row of mutually exclusive filter buttons. */
    private JPanel buildScopeSwitch() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_1, 0));
        panel.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        for (Scope option : Scope.values()) {
            JToggleButton button = new JToggleButton(option.label, option == scope);
            button.setFont(Theme.smallBoldFont());
            button.putClientProperty("JButton.buttonType", "toolBarButton");
            button.addActionListener(e -> {
                scope = option;
                applyScope();
            });
            group.add(button);
            panel.add(button);
        }
        return panel;
    }

    private void wireSelection() {
        table.onSelectionChanged(this::updateActionState);
    }

    private void updateActionState(Optional<LoanDetail> selected) {
        boolean outstanding = selected.map(detail -> detail.loan().isOutstanding()).orElse(false);
        boolean owesFine = selected.map(detail -> detail.outstandingFine().signum() > 0)
                .orElse(false);
        boolean renewable = selected
                .map(detail -> detail.loan().isOutstanding()
                        && !detail.loan().isOverdueOn(services.circulationService().today())
                        && detail.loan().renewals() < services.config().maxRenewals())
                .orElse(false);

        if (returnButton != null) {
            returnButton.setEnabled(outstanding);
        }
        if (payFineButton != null) {
            payFineButton.setEnabled(owesFine);
        }
        // A student may renew only their own loans, which is all this table shows them.
        renewButton.setEnabled(renewable);
    }

    private void applyScope() {
        LocalDate today = services.circulationService().today();
        Predicate<LoanDetail> predicate = switch (scope) {
            case OUTSTANDING -> detail -> detail.loan().isOutstanding();
            case OVERDUE -> detail -> detail.loan().isOverdueOn(today);
            case RETURNED -> detail -> detail.loan().isReturned();
            case ALL -> null;
        };
        table.setRowPredicate(predicate);
        updateSubtitle();
    }

    @Override
    public void refresh() {
        List<LoanDetail> loans = staffView
                ? services.circulationService().allLoans()
                : services.circulationService().loansForUser(account.id());
        table.setRows(loans);
        applyScope();
        updateActionState(table.selectedRow());
    }

    private void updateSubtitle() {
        LocalDate today = services.circulationService().today();
        List<LoanDetail> all = table.rows();
        long outstanding = all.stream().filter(detail -> detail.loan().isOutstanding()).count();
        long overdue = all.stream().filter(detail -> detail.loan().isOverdueOn(today)).count();
        BigDecimal owed = all.stream()
                .map(LoanDetail::outstandingFine)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String shown = staffView
                ? table.visibleRowCount() + " of " + all.size() + " loans shown"
                : table.visibleRowCount() + " of your " + all.size() + " loans shown";
        setSubtitle(shown + " · " + outstanding + " out now · " + overdue + " overdue · "
                + services.config().money(owed) + " in unpaid fines");
    }

    private void returnSelected() {
        Optional<LoanDetail> selected = table.selectedRow();
        if (selected.isEmpty()) {
            return;
        }
        LoanDetail detail = selected.get();
        BigDecimal fine = detail.fine();

        String question = fine.signum() > 0
                ? "Return \"" + detail.bookTitle() + "\" from " + detail.memberName() + "?\n\n"
                        + "It is " + Formats.plural((int) detail.loan()
                        .daysLate(services.circulationService().today()), "day", "days")
                        + " late, so a fine of " + services.config().money(fine) + " applies."
                : "Return \"" + detail.bookTitle() + "\" from " + detail.memberName()
                        + "? It is back on time, so there is no fine.";

        String confirmLabel = fine.signum() > 0 ? "Return and collect fine" : "Return";
        if (!Dialogs.confirm(this, "Return book", question, confirmLabel)) {
            return;
        }

        try {
            var receipt = services.circulationService()
                    .returnBook(detail.loan().id(), fine.signum() > 0);
            onDataChanged.run();
            Dialogs.showSuccess(this, receipt.wasLate()
                    ? "\"" + receipt.loan().bookTitle() + "\" is back. A fine of "
                            + services.config().money(receipt.fine()) + " was collected."
                    : "\"" + receipt.loan().bookTitle() + "\" is back on time. No fine due.");
        } catch (ValidationException e) {
            Dialogs.showValidationProblems(this, "That copy could not be returned.",
                    e.problems());
        } catch (RuntimeException e) {
            Dialogs.showError(this, "That copy could not be returned.", e);
        }
    }

    private void renewSelected() {
        Optional<LoanDetail> selected = table.selectedRow();
        if (selected.isEmpty()) {
            return;
        }
        LoanDetail detail = selected.get();
        try {
            var renewed = services.circulationService().renew(detail.loan().id());
            onDataChanged.run();
            Dialogs.showSuccess(this, "\"" + detail.bookTitle() + "\" is renewed until "
                    + Formats.date(renewed.dueDate()) + ". "
                    + (services.config().maxRenewals() - renewed.renewals())
                    + " renewals left.");
        } catch (ValidationException e) {
            Dialogs.showValidationProblems(this, "That loan could not be renewed.",
                    e.problems());
        } catch (RuntimeException e) {
            Dialogs.showError(this, "That loan could not be renewed.", e);
        }
    }

    private void collectFine() {
        Optional<LoanDetail> selected = table.selectedRow();
        if (selected.isEmpty()) {
            return;
        }
        LoanDetail detail = selected.get();
        BigDecimal owed = detail.outstandingFine();

        String input = javax.swing.JOptionPane.showInputDialog(this,
                "Amount to collect from " + detail.memberName() + " for \""
                        + detail.bookTitle() + "\".\nOutstanding: "
                        + services.config().money(owed),
                owed.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        if (input == null) {
            return;
        }
        try {
            services.circulationService().payFine(detail.loan().id(), new BigDecimal(input.trim()));
            onDataChanged.run();
            Dialogs.showSuccess(this, "Payment recorded.");
        } catch (NumberFormatException e) {
            Dialogs.showError(this, "\"" + input + "\" is not an amount. Enter a number "
                    + "such as 12.50.");
        } catch (ValidationException e) {
            Dialogs.showValidationProblems(this, "That payment was not recorded.", e.problems());
        } catch (RuntimeException e) {
            Dialogs.showError(this, "That payment was not recorded.", e);
        }
    }

    private java.awt.Window owningWindow() {
        return javax.swing.SwingUtilities.getWindowAncestor(this);
    }
}
