package com.example.library.service;

import com.example.library.config.AppConfig;
import com.example.library.model.Loan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Turns lateness into money.
 *
 * <p>A fine accrues for every day a copy is held beyond its due date, after any grace period.
 * For a returned book the fine is final; for one still out it keeps growing, so callers pass
 * the date they are evaluating against rather than relying on a hidden clock.
 */
public final class FinePolicy {

    private final BigDecimal perDay;
    private final int graceDays;

    public FinePolicy(AppConfig config) {
        this(config.finePerDay(), config.graceDays());
    }

    public FinePolicy(BigDecimal perDay, int graceDays) {
        this.perDay = Objects.requireNonNull(perDay, "perDay");
        this.graceDays = Math.max(0, graceDays);
    }

    /** Chargeable late days: days past the due date, less the grace period. */
    public long chargeableDays(Loan loan, LocalDate today) {
        return Math.max(0, loan.daysLate(today) - graceDays);
    }

    /** The fine the loan has accrued as of {@code today}, rounded to two places. */
    public BigDecimal fineFor(Loan loan, LocalDate today) {
        long days = chargeableDays(loan, today);
        if (days == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return perDay.multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);
    }

    /** The part of the fine that has not been paid yet. */
    public BigDecimal outstandingFineFor(Loan loan, LocalDate today) {
        BigDecimal owed = fineFor(loan, today).subtract(loan.finePaid());
        return owed.signum() > 0 ? owed : BigDecimal.ZERO.setScale(2);
    }

    public BigDecimal perDay() {
        return perDay;
    }

    public int graceDays() {
        return graceDays;
    }
}
