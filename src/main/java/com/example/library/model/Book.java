package com.example.library.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A catalogue entry. {@code totalCopies} is how many physical copies the library owns;
 * how many are on the shelf right now is derived from open loans, see {@link BookSummary}.
 *
 * @param id            database identity, {@code 0} for an unsaved record
 * @param isbn          optional ISBN-10 or ISBN-13, unique when present
 * @param title         the book title
 * @param author        primary author
 * @param publisher     publisher name
 * @param publishedYear year of publication, {@code null} when unknown
 * @param coverUrl      cover artwork URL, typically from Open Library
 * @param price         replacement price, used when reporting collection value
 * @param totalCopies   number of copies owned
 * @param addedDate     the day the title was catalogued
 */
public record Book(
        long id,
        String isbn,
        String title,
        String author,
        String publisher,
        Integer publishedYear,
        String coverUrl,
        BigDecimal price,
        int totalCopies,
        LocalDate addedDate) {

    public Book {
        Objects.requireNonNull(title, "title");
        price = price == null ? BigDecimal.ZERO : price;
    }

    /** A new, unsaved catalogue entry. */
    public static Book newEntry(String isbn, String title, String author, String publisher,
                                Integer publishedYear, String coverUrl, BigDecimal price,
                                int totalCopies) {
        return new Book(0, isbn, title, author, publisher, publishedYear, coverUrl, price,
                totalCopies, LocalDate.now());
    }

    /** The title with its author, as shown in pickers and confirmation prompts. */
    public String displayLabel() {
        return author == null || author.isBlank() ? title : title + " — " + author;
    }
}
