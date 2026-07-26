package com.example.library.repository;

import com.example.library.db.Database;
import com.example.library.db.RowMapper;
import com.example.library.model.Book;
import com.example.library.model.BookSummary;
import com.example.library.model.RankedTitle;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Reads and writes catalogue entries. */
public final class BookRepository {

    private static final String COLUMNS = """
            id, isbn, title, author, publisher, published_year, cover_url, price,
            total_copies, added_date
            """;

    /**
     * Selects catalogue entries alongside their circulation counts. Correlated subqueries keep
     * this a single round trip and avoid the row multiplication a join to {@code loans} causes.
     */
    private static final String SUMMARY_SELECT = """
            SELECT b.id, b.isbn, b.title, b.author, b.publisher, b.published_year, b.cover_url,
                   b.price, b.total_copies, b.added_date,
                   (SELECT COUNT(*) FROM loans l
                     WHERE l.book_id = b.id AND l.return_date IS NULL) AS on_loan,
                   (SELECT COUNT(*) FROM loans l WHERE l.book_id = b.id) AS times_issued
            FROM books b
            """;

    private static final RowMapper<Book> BOOK_MAPPER = rs -> new Book(
            rs.getLong("id"),
            rs.getString("isbn"),
            rs.getString("title"),
            rs.getString("author"),
            rs.getString("publisher"),
            Database.readInteger(rs, "published_year"),
            rs.getString("cover_url"),
            Database.readMoney(rs, "price"),
            rs.getInt("total_copies"),
            Database.readDate(rs, "added_date"));

    private static final RowMapper<BookSummary> SUMMARY_MAPPER = rs -> new BookSummary(
            BOOK_MAPPER.map(rs), rs.getInt("on_loan"), rs.getInt("times_issued"));

    private final Database database;

    public BookRepository(Database database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    public List<Book> findAll() {
        return database.query("SELECT " + COLUMNS + " FROM books ORDER BY title COLLATE NOCASE",
                BOOK_MAPPER);
    }

    /** Every catalogue entry with its live availability, ordered by title. */
    public List<BookSummary> findAllSummaries() {
        return database.query(SUMMARY_SELECT + " ORDER BY b.title COLLATE NOCASE", SUMMARY_MAPPER);
    }

    /** Catalogue entries with at least one copy on the shelf, for the issue picker. */
    public List<BookSummary> findAvailableSummaries() {
        return database.query(SUMMARY_SELECT + """
                WHERE b.total_copies > (SELECT COUNT(*) FROM loans l
                                         WHERE l.book_id = b.id AND l.return_date IS NULL)
                ORDER BY b.title COLLATE NOCASE
                """, SUMMARY_MAPPER);
    }

    public Optional<BookSummary> findSummaryById(long id) {
        return database.queryOne(SUMMARY_SELECT + " WHERE b.id = ?", SUMMARY_MAPPER, id);
    }

    public Optional<Book> findById(long id) {
        return database.queryOne("SELECT " + COLUMNS + " FROM books WHERE id = ?",
                BOOK_MAPPER, id);
    }

    public Optional<Book> findByIsbn(String isbn) {
        return database.queryOne("SELECT " + COLUMNS + " FROM books WHERE isbn = ?",
                BOOK_MAPPER, isbn);
    }

    /**
     * Inserts a catalogue entry.
     *
     * @return the generated identifier
     */
    public long insert(Book book) {
        return database.insert("""
                        INSERT INTO books (isbn, title, author, publisher, published_year, cover_url,
                                           price, total_copies, added_date)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                blankToNull(book.isbn()), book.title(), book.author(), book.publisher(),
                book.publishedYear(), book.coverUrl(), book.price(), book.totalCopies(),
                book.addedDate());
    }

    public void update(Book book) {
        database.update("""
                        UPDATE books SET isbn = ?, title = ?, author = ?, publisher = ?,
                                         published_year = ?, cover_url = ?, price = ?, total_copies = ?
                        WHERE id = ?
                        """,
                blankToNull(book.isbn()), book.title(), book.author(), book.publisher(),
                book.publishedYear(), book.coverUrl(), book.price(), book.totalCopies(),
                book.id());
    }

    public void delete(long bookId) {
        database.update("DELETE FROM books WHERE id = ?", bookId);
    }

    /** Whether some other catalogue entry already claims {@code isbn}. */
    public boolean isbnTaken(String isbn, long excludingBookId) {
        String value = blankToNull(isbn);
        if (value == null) {
            return false;
        }
        return database.count("SELECT COUNT(*) FROM books WHERE isbn = ? AND id <> ?",
                value, excludingBookId) > 0;
    }

    public int countTitles() {
        return (int) database.count("SELECT COUNT(*) FROM books");
    }

    public int countCopies() {
        return (int) database.count("SELECT COALESCE(SUM(total_copies), 0) FROM books");
    }

    public BigDecimal collectionValue() {
        return database.sum("SELECT COALESCE(SUM(price * total_copies), 0) FROM books");
    }

    /** Titles ranked by how often they have been borrowed. */
    public List<RankedTitle> mostBorrowed(int limit) {
        return database.query("""
                        SELECT b.title AS label, b.author AS sublabel, COUNT(l.id) AS loan_count
                        FROM books b
                        JOIN loans l ON l.book_id = b.id
                        GROUP BY b.id
                        ORDER BY loan_count DESC, b.title COLLATE NOCASE
                        LIMIT ?
                        """,
                rs -> new RankedTitle(rs.getString("label"), rs.getString("sublabel"),
                        rs.getInt("loan_count")),
                limit);
    }

    /** Titles the library owns but nobody has ever borrowed. */
    public List<Book> neverBorrowed(int limit) {
        return database.query("""
                        SELECT
                        """ + COLUMNS + """
                        FROM books
                        WHERE id NOT IN (SELECT book_id FROM loans)
                        ORDER BY added_date DESC
                        LIMIT ?
                        """, BOOK_MAPPER, limit);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
