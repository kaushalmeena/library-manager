package com.example.library.db;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Owns the SQLite connection and the handful of JDBC conveniences the repositories need.
 *
 * <p>This is a single-user desktop application, so one long-lived connection is kept open
 * for the lifetime of the process. Every method is {@code synchronized} because background
 * workers on the Swing thread pool may touch the database concurrently, and neither a JDBC
 * {@link Connection} nor a {@link PreparedStatement} is safe to share across threads.
 */
public final class Database implements AutoCloseable {

    private static final String SCHEMA_RESOURCE = "/db/schema.sql";
    private static final String SEED_RESOURCE = "/db/seed.sql";

    private final Connection connection;

    private Database(Connection connection) {
        this.connection = connection;
    }

    /**
     * Opens, creating the parent directory and the database file if needed.
     *
     * @param file where the SQLite database lives
     */
    public static Database openFile(Path file) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            return open("jdbc:sqlite:" + file.toAbsolutePath());
        } catch (IOException e) {
            throw new DataAccessException("Could not create the data directory for " + file, e);
        }
    }

    /** Opens a private in-memory database, used by the test suite. */
    public static Database openInMemory() {
        return open("jdbc:sqlite::memory:");
    }

    private static Database open(String jdbcUrl) {
        try {
            Connection connection = DriverManager.getConnection(jdbcUrl);
            try (Statement statement = connection.createStatement()) {
                // SQLite disables referential integrity per connection by default.
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("PRAGMA journal_mode = WAL");
                statement.execute("PRAGMA busy_timeout = 5000");
            }
            return new Database(connection);
        } catch (SQLException e) {
            throw new DataAccessException("Could not open the database at " + jdbcUrl, e);
        }
    }

    /**
     * Creates the schema when absent and loads demo data into a brand new database.
     *
     * @return {@code true} when this call seeded a fresh database
     */
    public synchronized boolean migrate() {
        runScript(SCHEMA_RESOURCE);
        boolean empty = count("SELECT COUNT(*) FROM users") == 0;
        if (empty) {
            runScript(SEED_RESOURCE);
        }
        return empty;
    }

    /**
     * Executes every statement in a bundled {@code .sql} resource.
     *
     * <p>Statements are split on semicolons, which is sufficient for the project's own
     * scripts: they contain no triggers, no {@code BEGIN ... END} blocks and no semicolons
     * inside string literals.
     */
    private void runScript(String resource) {
        String script = readResource(resource);
        try {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                for (String sql : script.split(";")) {
                    String trimmed = stripComments(sql).trim();
                    if (!trimmed.isEmpty()) {
                        statement.execute(trimmed);
                    }
                }
            }
            connection.commit();
        } catch (SQLException e) {
            rollbackQuietly();
            throw new DataAccessException("Failed to apply " + resource, e);
        } finally {
            restoreAutoCommit();
        }
    }

    private static String stripComments(String sql) {
        StringBuilder out = new StringBuilder(sql.length());
        for (String line : sql.split("\n")) {
            int comment = line.indexOf("--");
            out.append(comment >= 0 ? line.substring(0, comment) : line).append('\n');
        }
        return out.toString();
    }

    private static String readResource(String resource) {
        try (InputStream in = Database.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new DataAccessException("Missing bundled resource " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DataAccessException("Could not read " + resource, e);
        }
    }

    /** Runs a query and maps every row. */
    public synchronized <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapper.map(rs));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Query failed: " + sql, e);
        }
    }

    /** Runs a query expected to match at most one row. */
    public synchronized <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> rows = query(sql, mapper, params);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    /** Runs a single-value aggregate such as {@code SELECT COUNT(*) ...}. */
    public synchronized long count(String sql, Object... params) {
        return queryOne(sql, rs -> rs.getLong(1), params).orElse(0L);
    }

    /** Runs a single-value {@code SUM} that may be {@code NULL} for an empty set. */
    public synchronized BigDecimal sum(String sql, Object... params) {
        return queryOne(sql, rs -> {
            BigDecimal value = rs.getBigDecimal(1);
            return value == null ? BigDecimal.ZERO : value;
        }, params).orElse(BigDecimal.ZERO);
    }

    /**
     * Runs an {@code INSERT}, {@code UPDATE} or {@code DELETE}.
     *
     * @return the number of affected rows
     */
    public synchronized int update(String sql, Object... params) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Update failed: " + sql, e);
        }
    }

    /**
     * Runs an {@code INSERT} and returns the generated primary key.
     */
    public synchronized long insert(String sql, Object... params) {
        try (PreparedStatement statement =
                     connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, params);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            throw new DataAccessException("Insert returned no generated key: " + sql);
        } catch (SQLException e) {
            throw new DataAccessException("Insert failed: " + sql, e);
        }
    }

    /**
     * Runs {@code work} inside a transaction, committing on success and rolling back on any
     * exception. Nested calls join the outer transaction rather than starting a new one.
     */
    public synchronized <T> T transactional(Callable<T> work) {
        boolean outermost = isAutoCommit();
        try {
            if (outermost) {
                connection.setAutoCommit(false);
            }
            T result = work.call();
            if (outermost) {
                connection.commit();
            }
            return result;
        } catch (Exception e) {
            if (outermost) {
                rollbackQuietly();
            }
            if (e instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new DataAccessException("Transaction failed", e);
        } finally {
            if (outermost) {
                restoreAutoCommit();
            }
        }
    }

    private boolean isAutoCommit() {
        try {
            return connection.getAutoCommit();
        } catch (SQLException e) {
            throw new DataAccessException("Could not read the transaction state", e);
        }
    }

    private void rollbackQuietly() {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // Nothing useful can be done; the original failure is what matters.
        }
    }

    private void restoreAutoCommit() {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException ignored) {
            // Same as above: the caller is already dealing with a failure.
        }
    }

    /**
     * Binds positional parameters, translating the domain types the repositories use into
     * something SQLite understands.
     */
    private static void bind(PreparedStatement statement, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            int index = i + 1;
            Object value = params[i];
            if (value == null) {
                statement.setObject(index, null);
            } else if (value instanceof LocalDate date) {
                statement.setString(index, date.toString());
            } else if (value instanceof BigDecimal amount) {
                statement.setBigDecimal(index, amount);
            } else if (value instanceof Enum<?> constant) {
                statement.setString(index, constant.name());
            } else {
                statement.setObject(index, value);
            }
        }
    }

    /** Reads a nullable {@code DATE} column stored as an ISO string. */
    public static LocalDate readDate(ResultSet rs, String column) throws SQLException {
        String raw = rs.getString(column);
        return raw == null || raw.isBlank() ? null : LocalDate.parse(raw.trim());
    }

    /** Reads a nullable {@code INTEGER} column as a boxed {@link Integer}. */
    public static Integer readInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    /** Reads a money column, never returning {@code null}. */
    public static BigDecimal readMoney(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value == null ? BigDecimal.ZERO : value;
    }

    @Override
    public synchronized void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new DataAccessException("Could not close the database", e);
        }
    }
}
