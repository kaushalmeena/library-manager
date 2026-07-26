package com.example.library.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * One circulation event: a copy leaving the library and, eventually, coming back.
 *
 * <p>Loans are never deleted. A return stamps {@link #returnDate()}, so the table doubles
 * as the library's permanent borrowing history.
 *
 * @param id         database identity, {@code 0} for an unsaved record
 * @param bookId     the borrowed title
 * @param userId     the borrowing member
 * @param issueDate  the day the copy left the library
 * @param dueDate    the day it is expected back
 * @param returnDate the day it came back, {@code null} while still out
 * @param renewals   how many times the due date has been extended
 * @param finePaid   fine already settled against this loan
 */
public record Loan(
        long id,
        long bookId,
        long userId,
        LocalDate issueDate,
        LocalDate dueDate,
        LocalDate returnDate,
        int renewals,
        BigDecimal finePaid) {

    public Loan {
        Objects.requireNonNull(issueDate, "issueDate");
        Objects.requireNonNull(dueDate, "dueDate");
        finePaid = finePaid == null ? BigDecimal.ZERO : finePaid;
    }

    public boolean isReturned() {
        return returnDate != null;
    }

    public boolean isOutstanding() {
        return returnDate == null;
    }

    /** Whether the copy is still out and past its due date on {@code today}. */
    public boolean isOverdueOn(LocalDate today) {
        return isOutstanding() && today.isAfter(dueDate);
    }

    /**
     * Days the copy was, or is, held beyond the due date.
     *
     * <p>Measured to the return date for a closed loan and to {@code today} for an open one.
     * Never negative.
     */
    public long daysLate(LocalDate today) {
        LocalDate end = returnDate != null ? returnDate : today;
        long late = ChronoUnit.DAYS.between(dueDate, end);
        return Math.max(0, late);
    }

    /** Days remaining before the due date; negative once overdue. */
    public long daysUntilDue(LocalDate today) {
        return ChronoUnit.DAYS.between(today, dueDate);
    }

    /** How the loan should be labelled in the interface. */
    public LoanStatus status(LocalDate today, int dueSoonWindowDays) {
        if (isReturned()) {
            return returnDate.isAfter(dueDate) ? LoanStatus.RETURNED_LATE : LoanStatus.RETURNED;
        }
        if (today.isAfter(dueDate)) {
            return LoanStatus.OVERDUE;
        }
        return daysUntilDue(today) <= dueSoonWindowDays ? LoanStatus.DUE_SOON : LoanStatus.ON_LOAN;
    }
}
