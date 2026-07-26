package com.example.library.ui.support;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/** Shared formatting so dates, money and counts read the same on every screen. */
public final class Formats {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault());

    private Formats() {
    }

    /** A date as {@code 26 Jul 2026}, or an em dash when absent. */
    public static String date(LocalDate date) {
        return date == null ? "—" : DATE.format(date);
    }

    /** Formats a date held in an untyped table cell. */
    public static String dateCell(Object value) {
        return date((LocalDate) value);
    }

    /** An amount, or an em dash when it is zero, so tables are not littered with zeroes. */
    public static String moneyOrDash(String currencySymbol, BigDecimal amount) {
        if (amount == null || amount.signum() == 0) {
            return "—";
        }
        return currencySymbol + amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    /** A count with its noun pluralised, e.g. {@code 1 book} or {@code 3 books}. */
    public static String plural(int count, String singular, String plural) {
        return count + " " + (count == 1 ? singular : plural);
    }

    /**
     * A due date expressed relative to today, e.g. {@code due in 3 days} or
     * {@code 5 days overdue}.
     */
    public static String relativeDueDate(LocalDate dueDate, LocalDate today) {
        if (dueDate == null) {
            return "—";
        }
        long days = ChronoUnit.DAYS.between(today, dueDate);
        if (days == 0) {
            return "due today";
        }
        if (days > 0) {
            return days == 1 ? "due tomorrow" : "due in " + days + " days";
        }
        long late = -days;
        return late == 1 ? "1 day overdue" : late + " days overdue";
    }

    /** A short relative description of a past date, e.g. {@code 3 days ago}. */
    public static String timeAgo(LocalDate date, LocalDate today) {
        if (date == null) {
            return "—";
        }
        long days = ChronoUnit.DAYS.between(date, today);
        if (days <= 0) {
            return "today";
        }
        if (days == 1) {
            return "yesterday";
        }
        if (days < 30) {
            return days + " days ago";
        }
        long months = days / 30;
        if (months < 12) {
            return months == 1 ? "a month ago" : months + " months ago";
        }
        long years = days / 365;
        return years == 1 ? "a year ago" : years + " years ago";
    }
}
