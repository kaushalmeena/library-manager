package com.example.library.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A loan joined with the book and member it refers to, plus the fine it has accrued.
 * This is the row shape the circulation tables render.
 *
 * @param loan        the underlying circulation record
 * @param bookTitle   title of the borrowed book
 * @param bookAuthor  author of the borrowed book
 * @param isbn        ISBN of the borrowed book, may be {@code null}
 * @param memberName  name of the borrowing member
 * @param memberEmail email of the borrowing member
 * @param fine        fine accrued so far, computed by the fine policy
 */
public record LoanDetail(
        Loan loan,
        String bookTitle,
        String bookAuthor,
        String isbn,
        String memberName,
        String memberEmail,
        BigDecimal fine) {

    /** Returns a copy of this row with a recomputed fine. */
    public LoanDetail withFine(BigDecimal newFine) {
        return new LoanDetail(loan, bookTitle, bookAuthor, isbn, memberName, memberEmail, newFine);
    }

    public LoanStatus status(LocalDate today, int dueSoonWindowDays) {
        return loan.status(today, dueSoonWindowDays);
    }

    /** Fine still owed after subtracting whatever has already been paid. */
    public BigDecimal outstandingFine() {
        BigDecimal owed = fine.subtract(loan.finePaid());
        return owed.signum() > 0 ? owed : BigDecimal.ZERO;
    }
}
