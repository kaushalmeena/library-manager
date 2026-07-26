package com.example.library.service;

import com.example.library.config.AppConfig;
import com.example.library.db.Database;
import com.example.library.model.Book;
import com.example.library.model.BookSummary;
import com.example.library.model.Loan;
import com.example.library.model.LoanDetail;
import com.example.library.model.User;
import com.example.library.repository.BookRepository;
import com.example.library.repository.LoanRepository;
import com.example.library.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Issuing, returning and renewing books, and every rule that governs them.
 *
 * <p>Availability is always derived from open loans rather than stored as a counter, so it
 * cannot drift out of step with the circulation history. The current date arrives through a
 * supplier so tests can place themselves before or after a due date.
 */
public final class CirculationService {

    /** How close to its due date a loan has to be before it is flagged as due soon. */
    public static final int DUE_SOON_WINDOW_DAYS = 3;

    private final Database database;
    private final BookRepository books;
    private final UserRepository users;
    private final LoanRepository loans;
    private final FinePolicy finePolicy;
    private final AppConfig config;
    private final Supplier<LocalDate> clock;

    public CirculationService(Database database, BookRepository books, UserRepository users,
                              LoanRepository loans, FinePolicy finePolicy, AppConfig config) {
        this(database, books, users, loans, finePolicy, config, LocalDate::now);
    }

    public CirculationService(Database database, BookRepository books, UserRepository users,
                              LoanRepository loans, FinePolicy finePolicy, AppConfig config,
                              Supplier<LocalDate> clock) {
        this.database = Objects.requireNonNull(database, "database");
        this.books = Objects.requireNonNull(books, "books");
        this.users = Objects.requireNonNull(users, "users");
        this.loans = Objects.requireNonNull(loans, "loans");
        this.finePolicy = Objects.requireNonNull(finePolicy, "finePolicy");
        this.config = Objects.requireNonNull(config, "config");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** What a completed return produced, so the desk can tell the member what they owe. */
    public record ReturnReceipt(LoanDetail loan, BigDecimal fine, long daysLate) {

        public boolean wasLate() {
            return daysLate > 0;
        }
    }

    public LocalDate today() {
        return clock.get();
    }

    public FinePolicy finePolicy() {
        return finePolicy;
    }

    /**
     * Issues a copy of {@code bookId} to {@code userId}.
     *
     * <p>The availability check and the insert share a transaction so two desks cannot hand out
     * the same last copy.
     *
     * @return the newly created loan
     * @throws ValidationException when a library rule blocks the loan
     */
    public Loan issue(long bookId, long userId) {
        return database.transactional(() -> {
            Book book = books.findById(bookId).orElseThrow(
                    () -> new ValidationException("That book is no longer in the catalogue."));
            User member = users.findById(userId).orElseThrow(
                    () -> new ValidationException("That member no longer exists."));

            List<String> problems = new ArrayList<>();
            int onLoan = loans.countOutstandingForBook(bookId);
            if (book.totalCopies() - onLoan <= 0) {
                problems.add("Every copy of \"" + book.title() + "\" is currently on loan.");
            }
            if (loans.hasOpenLoan(bookId, userId)) {
                problems.add(member.name() + " is already holding a copy of this title.");
            }
            int held = loans.countOutstandingForUser(userId);
            if (held >= config.maxLoansPerMember()) {
                problems.add(member.name() + " already holds " + held + " books, the limit is "
                        + config.maxLoansPerMember() + ".");
            }
            int overdue = countOverdueForUser(userId);
            if (overdue > 0) {
                problems.add(member.name() + " has " + overdue
                        + (overdue == 1 ? " overdue book" : " overdue books")
                        + " and cannot borrow until they are returned.");
            }
            if (!problems.isEmpty()) {
                throw new ValidationException(problems);
            }

            LocalDate issueDate = today();
            LocalDate dueDate = issueDate.plusDays(config.loanDays());
            long loanId = loans.insert(bookId, userId, issueDate, dueDate);
            return loans.findById(loanId).orElseThrow(
                    () -> new IllegalStateException("Loan " + loanId + " vanished after insert"));
        });
    }

    /**
     * Closes a loan, charging whatever fine it accrued.
     *
     * @param loanId    the loan being closed
     * @param settleNow whether the fine is collected at the desk right now
     * @return a receipt describing the closed loan and its fine
     * @throws ValidationException when the loan is unknown or already closed
     */
    public ReturnReceipt returnBook(long loanId, boolean settleNow) {
        return database.transactional(() -> {
            Loan loan = loans.findById(loanId).orElseThrow(
                    () -> new ValidationException("That loan no longer exists."));
            if (loan.isReturned()) {
                throw new ValidationException("That copy has already been returned.");
            }

            LocalDate returnDate = today();
            Loan asReturned = new Loan(loan.id(), loan.bookId(), loan.userId(), loan.issueDate(),
                    loan.dueDate(), returnDate, loan.renewals(), loan.finePaid());
            BigDecimal fine = finePolicy.fineFor(asReturned, returnDate);
            BigDecimal paid = settleNow ? fine : loan.finePaid();

            if (!loans.markReturned(loanId, returnDate, paid)) {
                throw new ValidationException("That copy has already been returned.");
            }
            LoanDetail detail = loans.findDetailById(loanId).orElseThrow(
                    () -> new IllegalStateException("Loan " + loanId + " vanished after return"));
            long daysLate = finePolicy.chargeableDays(asReturned, returnDate);
            return new ReturnReceipt(detail.withFine(fine), fine, daysLate);
        });
    }

    /**
     * Extends an open loan by another full loan period.
     *
     * @return the renewed loan
     * @throws ValidationException when the loan is overdue or out of renewals
     */
    public Loan renew(long loanId) {
        return database.transactional(() -> {
            Loan loan = loans.findById(loanId).orElseThrow(
                    () -> new ValidationException("That loan no longer exists."));
            if (loan.isReturned()) {
                throw new ValidationException("That copy has already been returned.");
            }
            LocalDate now = today();
            if (loan.isOverdueOn(now)) {
                throw new ValidationException(
                        "An overdue loan cannot be renewed. Return the copy and issue it again.");
            }
            if (loan.renewals() >= config.maxRenewals()) {
                throw new ValidationException("This loan has already been renewed "
                        + loan.renewals() + " times, the limit is " + config.maxRenewals() + ".");
            }
            LocalDate newDueDate = loan.dueDate().plusDays(config.loanDays());
            if (!loans.extendDueDate(loanId, newDueDate)) {
                throw new ValidationException("That loan could not be renewed.");
            }
            return loans.findById(loanId).orElseThrow(
                    () -> new IllegalStateException("Loan " + loanId + " vanished after renewal"));
        });
    }

    /**
     * Records a fine payment.
     *
     * @throws ValidationException when the amount is not positive or exceeds what is owed
     */
    public void payFine(long loanId, BigDecimal amount) {
        database.transactional(() -> {
            if (amount == null || amount.signum() <= 0) {
                throw new ValidationException("Enter an amount greater than zero.");
            }
            Loan loan = loans.findById(loanId).orElseThrow(
                    () -> new ValidationException("That loan no longer exists."));
            BigDecimal owed = finePolicy.outstandingFineFor(loan, today());
            if (owed.signum() == 0) {
                throw new ValidationException("There is no outstanding fine on this loan.");
            }
            if (amount.compareTo(owed) > 0) {
                throw new ValidationException(
                        "That is more than the " + config.money(owed) + " outstanding.");
            }
            loans.recordFinePayment(loanId, amount);
            return null;
        });
    }

    /** Every loan ever recorded, with fines computed as of today. */
    public List<LoanDetail> allLoans() {
        return withFines(loans.findAllDetails());
    }

    /** Loans that have not come back yet, with fines computed as of today. */
    public List<LoanDetail> outstandingLoans() {
        return withFines(loans.findOutstandingDetails());
    }

    /** One member's complete borrowing history. */
    public List<LoanDetail> loansForUser(long userId) {
        return withFines(loans.findDetailsByUser(userId));
    }

    /** One member's open loans. */
    public List<LoanDetail> outstandingLoansForUser(long userId) {
        return withFines(loans.findOutstandingDetailsByUser(userId));
    }

    /** Open loans already past their due date. */
    public List<LoanDetail> overdueLoans() {
        return withFines(loans.findOverdueDetails(today()));
    }

    /** Open loans coming due within the due-soon window. */
    public List<LoanDetail> loansDueSoon() {
        LocalDate now = today();
        return withFines(loans.findDueBetween(now, now.plusDays(DUE_SOON_WINDOW_DAYS)));
    }

    /** Titles with at least one copy on the shelf. */
    public List<BookSummary> availableBooks() {
        return books.findAvailableSummaries();
    }

    /** Total unpaid fines owed by one member across all their loans. */
    public BigDecimal outstandingFineForUser(long userId) {
        LocalDate now = today();
        return loans.findDetailsByUser(userId).stream()
                .map(detail -> finePolicy.outstandingFineFor(detail.loan(), now))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** How many of a member's open loans are overdue. */
    public int countOverdueForUser(long userId) {
        LocalDate now = today();
        return (int) loans.findOutstandingDetailsByUser(userId).stream()
                .filter(detail -> detail.loan().isOverdueOn(now))
                .count();
    }

    /**
     * Whether a title can be removed. Copies still out have to come back first, otherwise the
     * cascade would erase the loans that record who is holding them.
     */
    public void checkBookCanBeDeleted(long bookId) {
        int onLoan = loans.countOutstandingForBook(bookId);
        if (onLoan > 0) {
            throw new ValidationException(onLoan + (onLoan == 1 ? " copy is" : " copies are")
                    + " still on loan. They must be returned before the title is removed.");
        }
    }

    /**
     * Whether an account can be removed, for the same reason as
     * {@link #checkBookCanBeDeleted(long)}.
     */
    public void checkUserCanBeDeleted(long userId) {
        int held = loans.countOutstandingForUser(userId);
        if (held > 0) {
            throw new ValidationException(held + (held == 1 ? " book is" : " books are")
                    + " still on loan to this member. They must be returned first.");
        }
    }

    /** Fills in the fine on each row, which the repository deliberately leaves at zero. */
    private List<LoanDetail> withFines(List<LoanDetail> details) {
        LocalDate now = today();
        List<LoanDetail> result = new ArrayList<>(details.size());
        for (LoanDetail detail : details) {
            result.add(detail.withFine(finePolicy.fineFor(detail.loan(), now)));
        }
        return result;
    }
}
