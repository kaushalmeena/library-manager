package com.example.library.db;

import com.example.library.model.Role;
import com.example.library.support.TestLibrary;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseTest {

    @Test
    @DisplayName("applies the schema and seeds demo data into a new database")
    void migratesAndSeeds() {
        try (TestLibrary library = TestLibrary.seeded()) {
            assertTrue(library.books.countTitles() > 0, "expected seeded books");
            assertTrue(library.users.countAll() > 0, "expected seeded users");
            assertTrue(library.loans.countOutstanding() > 0, "expected open loans");
        }
    }

    @Test
    @DisplayName("seeds a working sign-in for every demo account")
    void seededAccountsCanSignIn() {
        try (TestLibrary library = TestLibrary.seeded()) {
            for (String handle : new String[]{"admin", "librarian", "student"}) {
                assertTrue(library.auth.authenticate(handle, "password123".toCharArray())
                        .isPresent(), handle + " should be able to sign in");
            }
            assertEquals(Role.ADMIN,
                    library.users.findByHandle("admin").orElseThrow().role());
        }
    }

    @Test
    @DisplayName("seeds a spread of on-time, late, current and overdue loans")
    void seededLoansCoverEveryState() {
        try (TestLibrary library = TestLibrary.seeded()) {
            library.setToday(LocalDate.now());
            var all = library.circulation.allLoans();

            assertTrue(all.stream().anyMatch(detail -> detail.loan().isReturned()),
                    "expected returned loans");
            assertTrue(all.stream().anyMatch(detail -> detail.loan().isOutstanding()),
                    "expected open loans");
            assertTrue(all.stream()
                            .anyMatch(detail -> detail.loan().isOverdueOn(LocalDate.now())),
                    "expected overdue loans");
        }
    }

    @Test
    @DisplayName("running the migration a second time changes nothing")
    void migrationIsIdempotent() {
        try (TestLibrary library = TestLibrary.seeded()) {
            int titlesBefore = library.books.countTitles();
            int usersBefore = library.users.countAll();

            boolean seededAgain = library.database.migrate();

            assertEquals(false, seededAgain, "a populated database must not be reseeded");
            assertEquals(titlesBefore, library.books.countTitles());
            assertEquals(usersBefore, library.users.countAll());
        }
    }

    @Test
    @DisplayName("enforces foreign keys, so a loan cannot point at a missing book")
    void enforcesForeignKeys() {
        try (TestLibrary library = TestLibrary.empty()) {
            assertThrows(DataAccessException.class, () -> library.loans.insert(
                    999, 999, LocalDate.now(), LocalDate.now().plusDays(14)));
        }
    }

    @Test
    @DisplayName("cascades a deleted book to its loan history")
    void cascadesBookDeletion() {
        try (TestLibrary library = TestLibrary.empty()) {
            var member = library.addMember("alan");
            long bookId = library.addBook("Effective Java", 1);
            var loan = library.circulation.issue(bookId, member.id());
            library.circulation.returnBook(loan.id(), true);

            library.books.delete(bookId);

            assertTrue(library.loans.findById(loan.id()).isEmpty());
        }
    }

    @Test
    @DisplayName("rejects a second open loan of one title to the same member")
    void rejectsDuplicateOpenLoanAtTheDatabaseLevel() {
        try (TestLibrary library = TestLibrary.empty()) {
            var member = library.addMember("alan");
            long bookId = library.addBook("Effective Java", 5);
            LocalDate today = LocalDate.now();
            library.loans.insert(bookId, member.id(), today, today.plusDays(14));

            // The service refuses this too; this proves the unique index is the backstop.
            assertThrows(DataAccessException.class,
                    () -> library.loans.insert(bookId, member.id(), today, today.plusDays(14)));
        }
    }

    @Test
    @DisplayName("rolls a failed transaction back completely")
    void rollsBackFailedTransaction() {
        try (TestLibrary library = TestLibrary.empty()) {
            long bookId = library.addBook("Effective Java", 1);
            int titlesBefore = library.books.countTitles();

            assertThrows(IllegalStateException.class, () -> library.database.transactional(() -> {
                library.books.delete(bookId);
                throw new IllegalStateException("deliberate failure");
            }));

            assertEquals(titlesBefore, library.books.countTitles());
            assertTrue(library.books.findById(bookId).isPresent());
        }
    }

    @Test
    @DisplayName("commits a transaction that completes")
    void commitsSuccessfulTransaction() {
        try (TestLibrary library = TestLibrary.empty()) {
            Long bookId = library.database.transactional(
                    () -> library.addBook("Clean Code", 2));

            assertTrue(library.books.findById(bookId).isPresent());
        }
    }

    @Test
    @DisplayName("reads money and dates back in the types the model expects")
    void roundTripsTypes() {
        try (TestLibrary library = TestLibrary.empty()) {
            long id = library.catalogue.save(com.example.library.model.Book.newEntry(
                    "9780134685991", "Effective Java", "Joshua Bloch", "Addison-Wesley",
                    2017, null, new BigDecimal("45.99"), 3));

            var stored = library.books.findById(id).orElseThrow();
            assertEquals(new BigDecimal("45.99"), stored.price());
            assertEquals(2017, stored.publishedYear());
            assertEquals(LocalDate.now(), stored.addedDate());
        }
    }

    @Test
    @DisplayName("keeps a null published year null rather than turning it into zero")
    void preservesNullYear() {
        try (TestLibrary library = TestLibrary.empty()) {
            long id = library.catalogue.save(com.example.library.model.Book.newEntry(
                    null, "Undated Book", null, null, null, null, BigDecimal.ZERO, 1));

            assertEquals(null, library.books.findById(id).orElseThrow().publishedYear());
        }
    }
}
