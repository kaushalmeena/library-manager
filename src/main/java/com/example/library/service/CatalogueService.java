package com.example.library.service;

import com.example.library.model.Book;
import com.example.library.model.BookSummary;
import com.example.library.repository.BookRepository;
import com.example.library.repository.LoanRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Catalogue upkeep: validating, saving and removing titles. */
public final class CatalogueService {

    private static final int MIN_YEAR = 1000;

    private final BookRepository books;
    private final LoanRepository loans;

    public CatalogueService(BookRepository books, LoanRepository loans) {
        this.books = Objects.requireNonNull(books, "books");
        this.loans = Objects.requireNonNull(loans, "loans");
    }

    public List<BookSummary> allSummaries() {
        return books.findAllSummaries();
    }

    public Optional<BookSummary> findSummary(long bookId) {
        return books.findSummaryById(bookId);
    }

    /**
     * Validates and saves a title, inserting when {@code book.id()} is zero and updating
     * otherwise.
     *
     * @return the identifier of the saved title
     * @throws ValidationException when the details are invalid or the ISBN is already used
     */
    public long save(Book book) {
        Book cleaned = clean(book);
        validate(cleaned);
        if (cleaned.id() == 0) {
            return books.insert(cleaned);
        }
        books.update(cleaned);
        return cleaned.id();
    }

    /**
     * Removes a title.
     *
     * @throws ValidationException when copies are still on loan
     */
    public void delete(long bookId) {
        int onLoan = loans.countOutstandingForBook(bookId);
        if (onLoan > 0) {
            throw new ValidationException(onLoan + (onLoan == 1 ? " copy is" : " copies are")
                    + " still on loan. They must be returned before the title is removed.");
        }
        books.delete(bookId);
    }

    private Book clean(Book book) {
        return new Book(
                book.id(),
                normaliseIsbn(book.isbn()),
                trim(book.title()),
                emptyToNull(trim(book.author())),
                emptyToNull(trim(book.publisher())),
                book.publishedYear(),
                emptyToNull(trim(book.coverUrl())),
                book.price() == null ? BigDecimal.ZERO : book.price(),
                book.totalCopies(),
                book.addedDate() == null ? LocalDate.now() : book.addedDate());
    }

    private void validate(Book book) {
        List<String> problems = new ArrayList<>();
        if (book.title() == null || book.title().length() < 2) {
            problems.add("Title must be at least 2 characters.");
        }
        if (book.isbn() != null && !isValidIsbn(book.isbn())) {
            problems.add("ISBN must be 10 or 13 digits (an X is allowed as the last digit).");
        }
        if (book.price().signum() < 0) {
            problems.add("Price cannot be negative.");
        }
        if (book.totalCopies() < 0) {
            problems.add("Number of copies cannot be negative.");
        }
        Integer year = book.publishedYear();
        if (year != null && (year < MIN_YEAR || year > LocalDate.now().getYear() + 1)) {
            problems.add("Published year must be between " + MIN_YEAR + " and "
                    + (LocalDate.now().getYear() + 1) + ".");
        }
        if (book.id() != 0) {
            int onLoan = loans.countOutstandingForBook(book.id());
            if (book.totalCopies() < onLoan) {
                problems.add("There are " + onLoan + " copies on loan, so the total cannot drop "
                        + "below that.");
            }
        }
        if (problems.isEmpty() && book.isbn() != null && books.isbnTaken(book.isbn(), book.id())) {
            problems.add("Another title in the catalogue already uses that ISBN.");
        }
        if (!problems.isEmpty()) {
            throw new ValidationException(problems);
        }
    }

    /** Strips the separators people type into ISBN fields, e.g. {@code 978-0-13-468599-1}. */
    public static String normaliseIsbn(String isbn) {
        if (isbn == null) {
            return null;
        }
        String digits = isbn.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
        return digits.isEmpty() ? null : digits;
    }

    /** Whether a normalised ISBN has a plausible shape. Check digits are not verified. */
    public static boolean isValidIsbn(String normalisedIsbn) {
        if (normalisedIsbn == null) {
            return false;
        }
        return normalisedIsbn.matches("\\d{9}[\\dX]") || normalisedIsbn.matches("\\d{13}");
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
