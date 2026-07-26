package com.example.library.config;

import java.math.BigDecimal;
import java.nio.file.Path;

/**
 * Tunable library policy and storage locations.
 *
 * <p>Values are read from system properties so the application can be pointed at a
 * throw-away database or given a different loan policy without a rebuild, for example
 * {@code java -Dlibrary.loanDays=7 -jar library-manager.jar}.
 */
public final class AppConfig {

    private static final String DATA_DIR_PROPERTY = "library.dataDir";
    private static final String LOAN_DAYS_PROPERTY = "library.loanDays";
    private static final String FINE_PER_DAY_PROPERTY = "library.finePerDay";
    private static final String GRACE_DAYS_PROPERTY = "library.graceDays";
    private static final String MAX_LOANS_PROPERTY = "library.maxLoansPerMember";
    private static final String MAX_RENEWALS_PROPERTY = "library.maxRenewals";

    private final Path dataDirectory;
    private final int loanDays;
    private final BigDecimal finePerDay;
    private final int graceDays;
    private final int maxLoansPerMember;
    private final int maxRenewals;
    private final String currencySymbol;

    private AppConfig(Path dataDirectory, int loanDays, BigDecimal finePerDay, int graceDays,
                      int maxLoansPerMember, int maxRenewals, String currencySymbol) {
        this.dataDirectory = dataDirectory;
        this.loanDays = loanDays;
        this.finePerDay = finePerDay;
        this.graceDays = graceDays;
        this.maxLoansPerMember = maxLoansPerMember;
        this.maxRenewals = maxRenewals;
        this.currencySymbol = currencySymbol;
    }

    /** Configuration assembled from system properties, falling back to the defaults. */
    public static AppConfig fromSystemProperties() {
        Path defaultDir = Path.of(System.getProperty("user.home"), ".library-manager");
        return new AppConfig(
                Path.of(System.getProperty(DATA_DIR_PROPERTY, defaultDir.toString())),
                intProperty(LOAN_DAYS_PROPERTY, 14),
                decimalProperty(FINE_PER_DAY_PROPERTY, new BigDecimal("2.00")),
                intProperty(GRACE_DAYS_PROPERTY, 0),
                intProperty(MAX_LOANS_PROPERTY, 5),
                intProperty(MAX_RENEWALS_PROPERTY, 2),
                System.getProperty("library.currency", "₹"));
    }

    /** Configuration for tests and tooling that need an explicit data directory. */
    public static AppConfig forDataDirectory(Path dataDirectory) {
        return new AppConfig(dataDirectory, 14, new BigDecimal("2.00"), 0, 5, 2, "₹");
    }

    private static int intProperty(String key, int fallback) {
        String raw = System.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static BigDecimal decimalProperty(String key, BigDecimal fallback) {
        String raw = System.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public Path dataDirectory() {
        return dataDirectory;
    }

    public Path databaseFile() {
        return dataDirectory.resolve("library.db");
    }

    /** Number of days a freshly issued book may be kept. */
    public int loanDays() {
        return loanDays;
    }

    /** Fine charged for each late day beyond the grace period. */
    public BigDecimal finePerDay() {
        return finePerDay;
    }

    /** Late days forgiven before a fine starts accruing. */
    public int graceDays() {
        return graceDays;
    }

    /** Maximum number of books a member may hold at once. */
    public int maxLoansPerMember() {
        return maxLoansPerMember;
    }

    /** Maximum number of times a single loan may be renewed. */
    public int maxRenewals() {
        return maxRenewals;
    }

    public String currencySymbol() {
        return currencySymbol;
    }

    /** Formats an amount for display, e.g. {@code ₹12.00}. */
    public String money(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        return currencySymbol + value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
