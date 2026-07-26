package com.example.library.service;

import com.example.library.model.Book;
import com.example.library.model.BookSummary;
import com.example.library.model.User;
import com.example.library.support.TestLibrary;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogueServiceTest {

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
    @DisplayName("saves a title and reports it as fully available")
    void savesTitle() {
        long id = library.catalogue.save(Book.newEntry("978-0-13-468599-1", "Effective Java",
                "Joshua Bloch", "Addison-Wesley", 2017, null, new BigDecimal("45.99"), 3));

        BookSummary summary = library.catalogue.findSummary(id).orElseThrow();
        assertEquals("Effective Java", summary.book().title());
        assertEquals(3, summary.available());
        assertEquals(0, summary.onLoan());
        assertEquals(0, summary.timesIssued());
    }

    @Test
    @DisplayName("strips separators out of an ISBN before storing it")
    void normalisesIsbn() {
        long id = library.catalogue.save(Book.newEntry("978-0-13-468599-1", "Effective Java",
                "Joshua Bloch", "Addison-Wesley", 2017, null, new BigDecimal("45.99"), 1));

        assertEquals("9780134685991", library.books.findById(id).orElseThrow().isbn());
    }

    @Test
    @DisplayName("stores a blank ISBN as absent rather than as an empty string")
    void blankIsbnBecomesNull() {
        long id = library.catalogue.save(Book.newEntry("   ", "Untitled", null, null, null, null,
                BigDecimal.ZERO, 1));

        assertNull(library.books.findById(id).orElseThrow().isbn());
    }

    @Test
    @DisplayName("allows several titles with no ISBN at all")
    void allowsMultipleTitlesWithoutIsbn() {
        library.catalogue.save(Book.newEntry(null, "First Book", null, null, null, null,
                BigDecimal.ZERO, 1));
        library.catalogue.save(Book.newEntry(null, "Second Book", null, null, null, null,
                BigDecimal.ZERO, 1));

        assertEquals(2, library.books.countTitles());
    }

    @Test
    @DisplayName("refuses a second title with the same ISBN")
    void refusesDuplicateIsbn() {
        library.catalogue.save(Book.newEntry("9780134685991", "Effective Java", null, null, null,
                null, BigDecimal.ZERO, 1));

        ValidationException thrown = assertThrows(ValidationException.class,
                () -> library.catalogue.save(Book.newEntry("9780134685991", "A Different Book",
                        null, null, null, null, BigDecimal.ZERO, 1)));

        assertTrue(thrown.getMessage().contains("ISBN"), thrown.getMessage());
    }

    @Test
    @DisplayName("lets an existing title keep its own ISBN when edited")
    void allowsEditingWithoutIsbnClash() {
        long id = library.catalogue.save(Book.newEntry("9780134685991", "Effective Java", null,
                null, null, null, BigDecimal.ZERO, 2));
        Book stored = library.books.findById(id).orElseThrow();

        library.catalogue.save(new Book(stored.id(), stored.isbn(), "Effective Java, 3rd Edition",
                "Joshua Bloch", stored.publisher(), 2018, null, new BigDecimal("49.99"), 4,
                stored.addedDate()));

        Book updated = library.books.findById(id).orElseThrow();
        assertEquals("Effective Java, 3rd Edition", updated.title());
        assertEquals(4, updated.totalCopies());
    }

    @Test
    @DisplayName("rejects a title shorter than two characters")
    void rejectsShortTitle() {
        assertThrows(ValidationException.class, () -> library.catalogue.save(
                Book.newEntry(null, "X", null, null, null, null, BigDecimal.ZERO, 1)));
    }

    @Test
    @DisplayName("rejects a negative price or copy count")
    void rejectsNegativeNumbers() {
        assertThrows(ValidationException.class, () -> library.catalogue.save(
                Book.newEntry(null, "Some Book", null, null, null, null,
                        new BigDecimal("-1.00"), 1)));

        assertThrows(ValidationException.class, () -> library.catalogue.save(
                Book.newEntry(null, "Some Book", null, null, null, null, BigDecimal.ZERO, -2)));
    }

    @Test
    @DisplayName("rejects an implausible published year")
    void rejectsImplausibleYear() {
        assertThrows(ValidationException.class, () -> library.catalogue.save(
                Book.newEntry(null, "Some Book", null, null, 12, null, BigDecimal.ZERO, 1)));

        assertThrows(ValidationException.class, () -> library.catalogue.save(
                Book.newEntry(null, "Some Book", null, null, 9999, null, BigDecimal.ZERO, 1)));
    }

    @Test
    @DisplayName("refuses to cut the copy count below what is on loan")
    void refusesCopyCountBelowLoans() {
        long bookId = library.addBook("Effective Java", 2);
        User first = library.addMember("alan");
        User second = library.addMember("grace");
        library.circulation.issue(bookId, first.id());
        library.circulation.issue(bookId, second.id());
        Book stored = library.books.findById(bookId).orElseThrow();

        ValidationException thrown = assertThrows(ValidationException.class,
                () -> library.catalogue.save(new Book(stored.id(), stored.isbn(), stored.title(),
                        stored.author(), stored.publisher(), stored.publishedYear(),
                        stored.coverUrl(), stored.price(), 1, stored.addedDate())));

        assertTrue(thrown.getMessage().contains("on loan"), thrown.getMessage());
    }

    @ParameterizedTest
    @DisplayName("accepts well-formed ISBN-10 and ISBN-13 values")
    @ValueSource(strings = {"9780134685991", "0134685997", "013468599X", "978-0-13-468599-1",
            "0 13 468599 7"})
    void acceptsValidIsbns(String candidate) {
        assertTrue(CatalogueService.isValidIsbn(CatalogueService.normaliseIsbn(candidate)),
                candidate + " should be accepted");
    }

    @ParameterizedTest
    @DisplayName("rejects ISBN values of the wrong length or shape")
    @ValueSource(strings = {"123", "12345678901", "97801346859911", "abcdefghij", "X134685997"})
    void rejectsInvalidIsbns(String candidate) {
        assertFalse(CatalogueService.isValidIsbn(CatalogueService.normaliseIsbn(candidate)),
                candidate + " should be rejected");
    }

    @Test
    @DisplayName("normalising an empty ISBN yields nothing at all")
    void normalisesEmptyIsbn() {
        assertNull(CatalogueService.normaliseIsbn(null));
        assertNull(CatalogueService.normaliseIsbn(""));
        assertNull(CatalogueService.normaliseIsbn("  -- "));
    }
}
