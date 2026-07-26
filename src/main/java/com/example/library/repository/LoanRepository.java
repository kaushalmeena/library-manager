package com.example.library.repository;

import com.example.library.db.Database;
import com.example.library.db.RowMapper;
import com.example.library.model.Loan;
import com.example.library.model.LoanDetail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Reads and writes circulation records.
 *
 * <p>Rows are never deleted here: {@link #markReturned} closes a loan by stamping its return
 * date, which is what preserves the borrowing history.
 */
public final class LoanRepository {

    private static final String LOAN_COLUMNS =
            "id, book_id, user_id, issue_date, due_date, return_date, renewals, fine_paid";

    private static final String DETAIL_SELECT = """
            SELECT l.id, l.book_id, l.user_id, l.issue_date, l.due_date, l.return_date,
                   l.renewals, l.fine_paid,
                   b.title AS book_title, b.author AS book_author, b.isbn AS book_isbn,
                   u.name AS member_name, u.email AS member_email
            FROM loans l
            JOIN books b ON b.id = l.book_id
            JOIN users u ON u.id = l.user_id
            """;

    private static final RowMapper<Loan> LOAN_MAPPER = rs -> new Loan(
            rs.getLong("id"),
            rs.getLong("book_id"),
            rs.getLong("user_id"),
            Database.readDate(rs, "issue_date"),
            Database.readDate(rs, "due_date"),
            Database.readDate(rs, "return_date"),
            rs.getInt("renewals"),
            Database.readMoney(rs, "fine_paid"));

    /** Maps a joined row. The fine is left at zero for the fine policy to fill in. */
    private static final RowMapper<LoanDetail> DETAIL_MAPPER = rs -> new LoanDetail(
            LOAN_MAPPER.map(rs),
            rs.getString("book_title"),
            rs.getString("book_author"),
            rs.getString("book_isbn"),
            rs.getString("member_name"),
            rs.getString("member_email"),
            BigDecimal.ZERO);

    /** Open loans first, then most recent activity. */
    private static final String DEFAULT_ORDER =
            " ORDER BY (l.return_date IS NULL) DESC, l.due_date ASC, l.id DESC";

    private final Database database;

    public LoanRepository(Database database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    public Optional<Loan> findById(long loanId) {
        return database.queryOne("SELECT " + LOAN_COLUMNS + " FROM loans WHERE id = ?",
                LOAN_MAPPER, loanId);
    }

    public Optional<LoanDetail> findDetailById(long loanId) {
        return database.queryOne(DETAIL_SELECT + " WHERE l.id = ?", DETAIL_MAPPER, loanId);
    }

    /** Every loan the library has ever recorded. */
    public List<LoanDetail> findAllDetails() {
        return database.query(DETAIL_SELECT + DEFAULT_ORDER, DETAIL_MAPPER);
    }

    /** Loans that have not been returned yet. */
    public List<LoanDetail> findOutstandingDetails() {
        return database.query(DETAIL_SELECT + " WHERE l.return_date IS NULL" + DEFAULT_ORDER,
                DETAIL_MAPPER);
    }

    /** Every loan belonging to one member. */
    public List<LoanDetail> findDetailsByUser(long userId) {
        return database.query(DETAIL_SELECT + " WHERE l.user_id = ?" + DEFAULT_ORDER,
                DETAIL_MAPPER, userId);
    }

    /** Open loans belonging to one member. */
    public List<LoanDetail> findOutstandingDetailsByUser(long userId) {
        return database.query(
                DETAIL_SELECT + " WHERE l.user_id = ? AND l.return_date IS NULL" + DEFAULT_ORDER,
                DETAIL_MAPPER, userId);
    }

    /** Open loans already past their due date, soonest due first. */
    public List<LoanDetail> findOverdueDetails(LocalDate today) {
        return database.query(DETAIL_SELECT + """
                WHERE l.return_date IS NULL AND l.due_date < ?
                ORDER BY l.due_date ASC
                """, DETAIL_MAPPER, today);
    }

    /** Open loans due on or before {@code cutoff}, used for the "due soon" panel. */
    public List<LoanDetail> findDueBetween(LocalDate from, LocalDate cutoff) {
        return database.query(DETAIL_SELECT + """
                WHERE l.return_date IS NULL AND l.due_date >= ? AND l.due_date <= ?
                ORDER BY l.due_date ASC
                """, DETAIL_MAPPER, from, cutoff);
    }

    /**
     * Records a copy leaving the library.
     *
     * @return the generated loan identifier
     */
    public long insert(long bookId, long userId, LocalDate issueDate, LocalDate dueDate) {
        return database.insert("""
                        INSERT INTO loans (book_id, user_id, issue_date, due_date)
                        VALUES (?, ?, ?, ?)
                        """,
                bookId, userId, issueDate, dueDate);
    }

    /**
     * Closes an open loan.
     *
     * @return {@code true} when a row was still open and has now been closed
     */
    public boolean markReturned(long loanId, LocalDate returnDate, BigDecimal finePaid) {
        return database.update("""
                        UPDATE loans SET return_date = ?, fine_paid = ?
                        WHERE id = ? AND return_date IS NULL
                        """,
                returnDate, finePaid, loanId) == 1;
    }

    /**
     * Pushes an open loan's due date back and counts the renewal.
     *
     * @return {@code true} when the loan was open and has now been extended
     */
    public boolean extendDueDate(long loanId, LocalDate newDueDate) {
        return database.update("""
                        UPDATE loans SET due_date = ?, renewals = renewals + 1
                        WHERE id = ? AND return_date IS NULL
                        """,
                newDueDate, loanId) == 1;
    }

    /** Records a fine payment against a loan. */
    public void recordFinePayment(long loanId, BigDecimal amount) {
        database.update("UPDATE loans SET fine_paid = fine_paid + ? WHERE id = ?",
                amount, loanId);
    }

    public int countOutstanding() {
        return (int) database.count("SELECT COUNT(*) FROM loans WHERE return_date IS NULL");
    }

    public int countOutstandingForBook(long bookId) {
        return (int) database.count(
                "SELECT COUNT(*) FROM loans WHERE book_id = ? AND return_date IS NULL", bookId);
    }

    public int countOutstandingForUser(long userId) {
        return (int) database.count(
                "SELECT COUNT(*) FROM loans WHERE user_id = ? AND return_date IS NULL", userId);
    }

    /** Whether the member is already holding a copy of this title. */
    public boolean hasOpenLoan(long bookId, long userId) {
        return database.count("""
                SELECT COUNT(*) FROM loans
                WHERE book_id = ? AND user_id = ? AND return_date IS NULL
                """, bookId, userId) > 0;
    }

    public int countOverdue(LocalDate today) {
        return (int) database.count(
                "SELECT COUNT(*) FROM loans WHERE return_date IS NULL AND due_date < ?", today);
    }

    public int countDueBetween(LocalDate from, LocalDate cutoff) {
        return (int) database.count("""
                SELECT COUNT(*) FROM loans
                WHERE return_date IS NULL AND due_date >= ? AND due_date <= ?
                """, from, cutoff);
    }

    public int countIssuedSince(LocalDate since) {
        return (int) database.count("SELECT COUNT(*) FROM loans WHERE issue_date >= ?", since);
    }

    /** Loans per calendar month across the library, oldest first, for the activity chart. */
    public List<MonthlyCount> countByMonth(LocalDate since) {
        return database.query("""
                        SELECT strftime('%Y-%m', issue_date) AS month, COUNT(*) AS loan_count
                        FROM loans
                        WHERE issue_date >= ?
                        GROUP BY month
                        ORDER BY month
                        """,
                rs -> new MonthlyCount(rs.getString("month"), rs.getInt("loan_count")),
                since);
    }

    /** The same breakdown for a single member, for their own dashboard. */
    public List<MonthlyCount> countByMonthForUser(long userId, LocalDate since) {
        return database.query("""
                        SELECT strftime('%Y-%m', issue_date) AS month, COUNT(*) AS loan_count
                        FROM loans
                        WHERE user_id = ? AND issue_date >= ?
                        GROUP BY month
                        ORDER BY month
                        """,
                rs -> new MonthlyCount(rs.getString("month"), rs.getInt("loan_count")),
                userId, since);
    }

    /**
     * Loans grouped by the month they were issued in.
     *
     * @param month     the month, formatted {@code yyyy-MM}
     * @param loanCount how many loans started that month
     */
    public record MonthlyCount(String month, int loanCount) {
    }
}
