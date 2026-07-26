package com.example.library.model;

/**
 * A catalogue entry together with its live circulation counts, which is what the
 * catalogue table and availability badges are built from.
 *
 * @param book        the catalogue entry
 * @param onLoan      copies currently issued and not yet returned
 * @param timesIssued how often the title has ever been borrowed
 */
public record BookSummary(Book book, int onLoan, int timesIssued) {

    /** Copies sitting on the shelf right now. */
    public int available() {
        return Math.max(0, book.totalCopies() - onLoan);
    }

    public boolean isAvailable() {
        return available() > 0;
    }

    /** Availability as shown in the catalogue, e.g. {@code 3 of 5}. */
    public String availabilityLabel() {
        return available() + " of " + book.totalCopies();
    }
}
