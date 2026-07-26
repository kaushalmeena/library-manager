package com.example.library.service;

import com.example.library.model.LibraryStats;
import com.example.library.model.RankedTitle;
import com.example.library.repository.BookRepository;
import com.example.library.repository.LoanRepository;
import com.example.library.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Aggregates the numbers the dashboard shows. */
public final class StatsService {

    private final BookRepository books;
    private final UserRepository users;
    private final LoanRepository loans;
    private final CirculationService circulation;

    public StatsService(BookRepository books, UserRepository users, LoanRepository loans,
                        CirculationService circulation) {
        this.books = Objects.requireNonNull(books, "books");
        this.users = Objects.requireNonNull(users, "users");
        this.loans = Objects.requireNonNull(loans, "loans");
        this.circulation = Objects.requireNonNull(circulation, "circulation");
    }

    /** Library-wide totals as of today. */
    public LibraryStats libraryStats() {
        LocalDate today = circulation.today();
        BigDecimal outstandingFines = circulation.outstandingLoans().stream()
                .map(detail -> circulation.finePolicy()
                        .outstandingFineFor(detail.loan(), today))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new LibraryStats(
                books.countTitles(),
                books.countCopies(),
                loans.countOutstanding(),
                loans.countOverdue(today),
                loans.countDueBetween(today, today.plusDays(CirculationService.DUE_SOON_WINDOW_DAYS)),
                users.countAll(),
                loans.countIssuedSince(today.minusDays(30)),
                outstandingFines,
                books.collectionValue());
    }

    /** A single member's headline numbers, for the student dashboard. */
    public MemberStats memberStats(long userId) {
        LocalDate today = circulation.today();
        var history = circulation.loansForUser(userId);
        int outstanding = 0;
        int overdue = 0;
        for (var detail : history) {
            if (detail.loan().isOutstanding()) {
                outstanding++;
                if (detail.loan().isOverdueOn(today)) {
                    overdue++;
                }
            }
        }
        return new MemberStats(history.size(), outstanding, overdue,
                circulation.outstandingFineForUser(userId));
    }

    /**
     * One member's headline numbers.
     *
     * @param borrowedEverTotal every loan the member has ever taken out
     * @param currentlyHeld     copies they are holding right now
     * @param overdue           how many of those are overdue
     * @param finesOwed         unpaid fines across their whole history
     */
    public record MemberStats(int borrowedEverTotal, int currentlyHeld, int overdue,
                              BigDecimal finesOwed) {
    }

    public List<RankedTitle> mostBorrowedTitles(int limit) {
        return books.mostBorrowed(limit);
    }

    public List<RankedTitle> topBorrowers(int limit) {
        return users.topBorrowers(limit);
    }

    /**
     * Loans issued across the library in each of the last {@code months} calendar months, oldest
     * first. Months with no activity are included as zero so the chart keeps an even spacing.
     */
    public List<MonthlyActivity> monthlyActivity(int months) {
        LocalDate start = startOfWindow(months);
        return fillMonths(months, loans.countByMonth(start));
    }

    /** The same series for one member, so a personal dashboard shows their own borrowing. */
    public List<MonthlyActivity> monthlyActivityForUser(long userId, int months) {
        LocalDate start = startOfWindow(months);
        return fillMonths(months, loans.countByMonthForUser(userId, start));
    }

    private LocalDate startOfWindow(int months) {
        return YearMonth.from(circulation.today()).minusMonths(months - 1L).atDay(1);
    }

    /** Expands sparse month counts into one entry per month, zero-filling the quiet ones. */
    private List<MonthlyActivity> fillMonths(int months, List<LoanRepository.MonthlyCount> rows) {
        YearMonth start = YearMonth.from(circulation.today()).minusMonths(months - 1L);
        Map<String, Integer> counts = new HashMap<>();
        for (LoanRepository.MonthlyCount row : rows) {
            counts.put(row.month(), row.loanCount());
        }
        List<MonthlyActivity> series = new ArrayList<>(months);
        for (int i = 0; i < months; i++) {
            YearMonth month = start.plusMonths(i);
            String key = String.format("%04d-%02d", month.getYear(), month.getMonthValue());
            series.add(new MonthlyActivity(month, counts.getOrDefault(key, 0)));
        }
        return series;
    }

    /**
     * Loans issued in one calendar month.
     *
     * @param month the month in question
     * @param loans how many loans started in it
     */
    public record MonthlyActivity(YearMonth month, int loans) {

        /** A short axis label such as {@code Jul}. */
        public String shortLabel() {
            return month.getMonth().getDisplayName(java.time.format.TextStyle.SHORT,
                    java.util.Locale.getDefault());
        }
    }
}
