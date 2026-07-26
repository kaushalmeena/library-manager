package com.example.library.model;

import java.math.BigDecimal;

/**
 * Library-wide totals shown on the dashboard.
 *
 * @param titles            distinct catalogue entries
 * @param copies            physical copies owned across all titles
 * @param onLoan            copies currently issued
 * @param overdue           issued copies past their due date
 * @param dueSoon           issued copies approaching their due date
 * @param members           registered accounts
 * @param loansThisMonth    loans issued in the last 30 days
 * @param outstandingFines  fines accrued but not yet settled
 * @param collectionValue   replacement value of the whole collection
 */
public record LibraryStats(
        int titles,
        int copies,
        int onLoan,
        int overdue,
        int dueSoon,
        int members,
        int loansThisMonth,
        BigDecimal outstandingFines,
        BigDecimal collectionValue) {

    /** Copies available to borrow right now. */
    public int available() {
        return Math.max(0, copies - onLoan);
    }

    /** Share of the collection currently out, as a percentage. */
    public int utilisationPercent() {
        return copies == 0 ? 0 : Math.round(onLoan * 100f / copies);
    }
}
