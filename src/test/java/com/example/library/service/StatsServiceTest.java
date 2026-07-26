package com.example.library.service;

import com.example.library.model.LibraryStats;
import com.example.library.model.User;
import com.example.library.support.TestLibrary;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatsServiceTest {

    private TestLibrary library;

    @BeforeEach
    void setUp() {
        library = TestLibrary.empty();
    }

    @AfterEach
    void tearDown() {
        library.close();
    }

    @Test
    @DisplayName("counts titles, copies and members across the library")
    void countsCollection() {
        library.addBook("Effective Java", 3);
        library.addBook("Clean Code", 2);
        library.addMember("alan");
        library.addMember("grace");

        LibraryStats stats = library.stats.libraryStats();

        assertEquals(2, stats.titles());
        assertEquals(5, stats.copies());
        assertEquals(2, stats.members());
        assertEquals(0, stats.onLoan());
        assertEquals(5, stats.available());
    }

    @Test
    @DisplayName("tracks how much of the collection is out")
    void tracksUtilisation() {
        long bookId = library.addBook("Effective Java", 4);
        User first = library.addMember("alan");
        User second = library.addMember("grace");
        library.circulation.issue(bookId, first.id());
        library.circulation.issue(bookId, second.id());

        LibraryStats stats = library.stats.libraryStats();

        assertEquals(2, stats.onLoan());
        assertEquals(2, stats.available());
        assertEquals(50, stats.utilisationPercent());
    }

    @Test
    @DisplayName("reports zero utilisation for an empty collection rather than dividing by zero")
    void handlesEmptyCollection() {
        LibraryStats stats = library.stats.libraryStats();

        assertEquals(0, stats.utilisationPercent());
        assertEquals(0, stats.available());
        assertEquals(0, stats.outstandingFines().signum());
    }

    @Test
    @DisplayName("separates overdue loans from those merely due soon")
    void separatesOverdueFromDueSoon() {
        User member = library.addMember("alan");
        library.circulation.issue(library.addBook("First", 1), member.id());
        library.advanceDays(library.config.loanDays() - 2);

        assertEquals(1, library.stats.libraryStats().dueSoon());
        assertEquals(0, library.stats.libraryStats().overdue());

        library.advanceDays(3);
        assertEquals(0, library.stats.libraryStats().dueSoon());
        assertEquals(1, library.stats.libraryStats().overdue());
    }

    @Test
    @DisplayName("totals the fines the library is owed")
    void totalsOutstandingFines() {
        User first = library.addMember("alan");
        User second = library.addMember("grace");
        library.circulation.issue(library.addBook("First", 1), first.id());
        library.circulation.issue(library.addBook("Second", 1), second.id());
        library.advanceDays(library.config.loanDays() + 3);

        // Two loans, three days late each, at two per day.
        assertEquals(new BigDecimal("12.00"), library.stats.libraryStats().outstandingFines());
    }

    @Test
    @DisplayName("values the collection by price times copies")
    void valuesCollection() {
        library.catalogue.save(com.example.library.model.Book.newEntry(null, "Priced Book",
                null, null, null, null, new BigDecimal("10.00"), 3));
        library.catalogue.save(com.example.library.model.Book.newEntry(null, "Another Book",
                null, null, null, null, new BigDecimal("5.50"), 2));

        assertEquals(new BigDecimal("41.0"), library.stats.libraryStats().collectionValue());
    }

    @Test
    @DisplayName("summarises one member's own borrowing")
    void summarisesMember() {
        User member = library.addMember("alan");
        long first = library.addBook("First", 1);
        long second = library.addBook("Second", 1);
        var returned = library.circulation.issue(first, member.id());
        library.circulation.returnBook(returned.id(), true);
        library.circulation.issue(second, member.id());

        StatsService.MemberStats stats = library.stats.memberStats(member.id());

        assertEquals(2, stats.borrowedEverTotal());
        assertEquals(1, stats.currentlyHeld());
        assertEquals(0, stats.overdue());
        assertEquals(0, stats.finesOwed().signum());
    }

    @Test
    @DisplayName("ranks titles by how often they have been borrowed")
    void ranksMostBorrowed() {
        long popular = library.addBook("Popular Title", 3);
        long quiet = library.addBook("Quiet Title", 3);
        for (String username : List.of("alan", "grace", "linus")) {
            User member = library.addMember(username);
            var loan = library.circulation.issue(popular, member.id());
            library.circulation.returnBook(loan.id(), true);
        }
        User single = library.addMember("maggie");
        library.circulation.issue(quiet, single.id());

        var ranked = library.stats.mostBorrowedTitles(5);

        assertEquals("Popular Title", ranked.get(0).label());
        assertEquals(3, ranked.get(0).count());
        assertEquals("Quiet Title", ranked.get(1).label());
    }

    @Test
    @DisplayName("ranks members by how much they have borrowed")
    void ranksTopBorrowers() {
        long first = library.addBook("First", 5);
        long second = library.addBook("Second", 5);
        User keen = library.addMember("alan");
        User casual = library.addMember("grace");
        library.circulation.issue(first, keen.id());
        library.circulation.issue(second, keen.id());
        library.circulation.issue(first, casual.id());

        var ranked = library.stats.topBorrowers(5);

        assertEquals("Test alan", ranked.get(0).label());
        assertEquals(2, ranked.get(0).count());
    }

    @Test
    @DisplayName("returns one activity point per month, including quiet months")
    void fillsQuietMonthsWithZero() {
        User member = library.addMember("alan");
        library.circulation.issue(library.addBook("Only Loan", 1), member.id());

        var activity = library.stats.monthlyActivity(6);

        assertEquals(6, activity.size());
        assertEquals(1, activity.get(activity.size() - 1).loans(),
                "the newest month holds the loan just issued");
        assertTrue(activity.stream().limit(5).allMatch(month -> month.loans() == 0),
                "earlier months should report zero rather than be missing");
    }

    @Test
    @DisplayName("charts one member's own borrowing separately from the library's")
    void chartsMemberActivitySeparately() {
        User mine = library.addMember("alan");
        User other = library.addMember("grace");
        library.circulation.issue(library.addBook("Mine", 1), mine.id());
        library.circulation.issue(library.addBook("Theirs A", 1), other.id());
        library.circulation.issue(library.addBook("Theirs B", 1), other.id());

        int libraryTotal = library.stats.monthlyActivity(6).stream()
                .mapToInt(StatsService.MonthlyActivity::loans).sum();
        int mineTotal = library.stats.monthlyActivityForUser(mine.id(), 6).stream()
                .mapToInt(StatsService.MonthlyActivity::loans).sum();

        assertEquals(3, libraryTotal);
        assertEquals(1, mineTotal);
        assertEquals(6, library.stats.monthlyActivityForUser(mine.id(), 6).size());
    }

    @Test
    @DisplayName("counts loans issued in the last thirty days")
    void countsRecentLoans() {
        User member = library.addMember("alan");
        library.circulation.issue(library.addBook("Recent", 1), member.id());

        assertEquals(1, library.stats.libraryStats().loansThisMonth());

        library.advanceDays(45);
        assertEquals(0, library.stats.libraryStats().loansThisMonth());
    }
}
