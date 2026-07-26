package com.example.library.support;

import com.example.library.config.AppConfig;
import com.example.library.db.Database;
import com.example.library.model.Book;
import com.example.library.model.Role;
import com.example.library.model.User;
import com.example.library.repository.BookRepository;
import com.example.library.repository.LoanRepository;
import com.example.library.repository.UserRepository;
import com.example.library.service.AuthService;
import com.example.library.service.CatalogueService;
import com.example.library.service.CirculationService;
import com.example.library.service.FinePolicy;
import com.example.library.service.StatsService;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;

/**
 * A complete object graph over a throw-away in-memory database, with a movable clock so tests
 * can step past a due date.
 */
public final class TestLibrary implements AutoCloseable {

    public final AppConfig config;
    public final Database database;
    public final UserRepository users;
    public final BookRepository books;
    public final LoanRepository loans;
    public final AuthService auth;
    public final CatalogueService catalogue;
    public final CirculationService circulation;
    public final StatsService stats;

    private LocalDate today = LocalDate.of(2026, 3, 1);

    private TestLibrary(boolean seeded) {
        this.config = AppConfig.forDataDirectory(Path.of("target", "test-data"));
        this.database = Database.openInMemory();
        if (seeded) {
            database.migrate();
        } else {
            // Apply the schema, then clear the demo rows so tests start from a known empty state.
            database.migrate();
            database.update("DELETE FROM loans");
            database.update("DELETE FROM books");
            database.update("DELETE FROM users");
        }
        this.users = new UserRepository(database);
        this.books = new BookRepository(database);
        this.loans = new LoanRepository(database);
        FinePolicy finePolicy = new FinePolicy(config);
        this.auth = new AuthService(users);
        this.catalogue = new CatalogueService(books, loans);
        this.circulation = new CirculationService(database, books, users, loans, finePolicy,
                config, () -> today);
        this.stats = new StatsService(books, users, loans, circulation);
    }

    /** An empty library: schema applied, no rows. */
    public static TestLibrary empty() {
        return new TestLibrary(false);
    }

    /** A library holding the bundled demo data. */
    public static TestLibrary seeded() {
        return new TestLibrary(true);
    }

    public LocalDate today() {
        return today;
    }

    /** Moves the clock the circulation service reads. */
    public void setToday(LocalDate date) {
        this.today = date;
    }

    /** Moves the clock forward. */
    public void advanceDays(long days) {
        this.today = this.today.plusDays(days);
    }

    /** Registers a member and returns the saved account. */
    public User addMember(String username) {
        return auth.register("Test " + username, username + "@library.test", "9876543210",
                username, "password123".toCharArray(), Role.STUDENT);
    }

    /** Catalogues a title with the given number of copies and returns its identifier. */
    public long addBook(String title, int copies) {
        return catalogue.save(Book.newEntry(null, title, "Some Author", "Some Publisher", 2020,
                null, new BigDecimal("10.00"), copies));
    }

    @Override
    public void close() {
        database.close();
    }
}
