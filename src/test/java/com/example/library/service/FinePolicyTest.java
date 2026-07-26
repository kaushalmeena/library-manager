package com.example.library.service;

import com.example.library.model.Loan;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FinePolicyTest {

    private static final LocalDate ISSUED = LocalDate.of(2026, 3, 1);
    private static final LocalDate DUE = LocalDate.of(2026, 3, 15);

    private static final FinePolicy NO_GRACE = new FinePolicy(new BigDecimal("2.00"), 0);
    private static final FinePolicy THREE_DAY_GRACE = new FinePolicy(new BigDecimal("2.00"), 3);

    private static Loan openLoan() {
        return new Loan(1, 1, 1, ISSUED, DUE, null, 0, BigDecimal.ZERO);
    }

    private static Loan returnedOn(LocalDate returnDate) {
        return new Loan(1, 1, 1, ISSUED, DUE, returnDate, 0, BigDecimal.ZERO);
    }

    @ParameterizedTest
    @DisplayName("charges the daily rate for each day past the due date")
    @CsvSource({
            "2026-03-10, 0.00",
            "2026-03-15, 0.00",
            "2026-03-16, 2.00",
            "2026-03-20, 10.00",
            "2026-04-15, 62.00"
    })
    void chargesPerLateDay(String today, String expected) {
        BigDecimal fine = NO_GRACE.fineFor(openLoan(), LocalDate.parse(today));

        assertEquals(new BigDecimal(expected), fine);
    }

    @Test
    @DisplayName("stops the clock on the day a book is returned")
    void stopsAtReturnDate() {
        Loan returned = returnedOn(LocalDate.of(2026, 3, 18));

        // Three days late, and the fine does not keep growing afterwards.
        assertEquals(new BigDecimal("6.00"),
                NO_GRACE.fineFor(returned, LocalDate.of(2026, 3, 18)));
        assertEquals(new BigDecimal("6.00"),
                NO_GRACE.fineFor(returned, LocalDate.of(2026, 6, 1)));
    }

    @Test
    @DisplayName("charges nothing for a book returned early or exactly on time")
    void noFineWhenOnTime() {
        assertEquals(0, NO_GRACE.fineFor(returnedOn(LocalDate.of(2026, 3, 10)), DUE).signum());
        assertEquals(0, NO_GRACE.fineFor(returnedOn(DUE), DUE).signum());
    }

    @Test
    @DisplayName("forgives late days inside the grace period")
    void appliesGracePeriod() {
        LocalDate twoDaysLate = DUE.plusDays(2);
        LocalDate fiveDaysLate = DUE.plusDays(5);

        assertEquals(0, THREE_DAY_GRACE.fineFor(openLoan(), twoDaysLate).signum());
        assertEquals(0, THREE_DAY_GRACE.fineFor(openLoan(), DUE.plusDays(3)).signum());
        // Five days late, three forgiven, so two chargeable days remain.
        assertEquals(new BigDecimal("4.00"), THREE_DAY_GRACE.fineFor(openLoan(), fiveDaysLate));
    }

    @Test
    @DisplayName("counts chargeable days separately from calendar lateness")
    void countsChargeableDays() {
        LocalDate fiveDaysLate = DUE.plusDays(5);

        assertEquals(5, openLoan().daysLate(fiveDaysLate));
        assertEquals(5, NO_GRACE.chargeableDays(openLoan(), fiveDaysLate));
        assertEquals(2, THREE_DAY_GRACE.chargeableDays(openLoan(), fiveDaysLate));
    }

    @Test
    @DisplayName("subtracts what has already been paid")
    void subtractsPayments() {
        Loan partlyPaid = new Loan(1, 1, 1, ISSUED, DUE, null, 0, new BigDecimal("4.00"));
        LocalDate fiveDaysLate = DUE.plusDays(5);

        assertEquals(new BigDecimal("10.00"), NO_GRACE.fineFor(partlyPaid, fiveDaysLate));
        assertEquals(new BigDecimal("6.00"), NO_GRACE.outstandingFineFor(partlyPaid, fiveDaysLate));
    }

    @Test
    @DisplayName("never reports a negative balance when overpaid")
    void neverGoesNegative() {
        Loan overpaid = new Loan(1, 1, 1, ISSUED, DUE, null, 0, new BigDecimal("50.00"));

        assertEquals(0, NO_GRACE.outstandingFineFor(overpaid, DUE.plusDays(1)).signum());
    }

    @Test
    @DisplayName("always reports amounts to two decimal places")
    void alwaysTwoDecimalPlaces() {
        assertEquals("0.00", NO_GRACE.fineFor(openLoan(), DUE).toPlainString());
        assertEquals("2.00", NO_GRACE.fineFor(openLoan(), DUE.plusDays(1)).toPlainString());
    }

    @Test
    @DisplayName("rounds a fractional daily rate to the nearest penny")
    void roundsFractionalRates() {
        FinePolicy oddRate = new FinePolicy(new BigDecimal("0.335"), 0);

        assertEquals(new BigDecimal("1.01"), oddRate.fineFor(openLoan(), DUE.plusDays(3)));
    }
}
