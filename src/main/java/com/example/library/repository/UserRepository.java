package com.example.library.repository;

import com.example.library.db.Database;
import com.example.library.db.RowMapper;
import com.example.library.model.RankedTitle;
import com.example.library.model.Role;
import com.example.library.model.User;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Reads and writes library accounts. */
public final class UserRepository {

    /** The stored secret for one account, only ever used while verifying a sign-in. */
    public record StoredCredentials(long userId, String passwordHash) {
    }

    private static final String COLUMNS =
            "id, name, email, mobile, username, role, created_date";

    private static final RowMapper<User> MAPPER = rs -> new User(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("mobile"),
            rs.getString("username"),
            Role.fromDatabase(rs.getString("role")),
            Database.readDate(rs, "created_date"));

    private final Database database;

    public UserRepository(Database database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    public List<User> findAll() {
        return database.query("SELECT " + COLUMNS + " FROM users ORDER BY name COLLATE NOCASE",
                MAPPER);
    }

    public Optional<User> findById(long id) {
        return database.queryOne("SELECT " + COLUMNS + " FROM users WHERE id = ?", MAPPER, id);
    }

    /** Looks an account up by either its username or its email address. */
    public Optional<User> findByHandle(String handle) {
        return database.queryOne(
                "SELECT " + COLUMNS + " FROM users WHERE username = ? OR email = ?",
                MAPPER, handle, handle);
    }

    /** Fetches the password hash for a sign-in handle. */
    public Optional<StoredCredentials> findCredentials(String handle) {
        return database.queryOne(
                "SELECT id, password_hash FROM users WHERE username = ? OR email = ?",
                rs -> new StoredCredentials(rs.getLong("id"), rs.getString("password_hash")),
                handle, handle);
    }

    /**
     * Inserts a new account.
     *
     * @return the generated identifier
     */
    public long insert(User user, String passwordHash) {
        return database.insert("""
                        INSERT INTO users (name, email, mobile, username, password_hash, role, created_date)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                user.name(), user.email(), user.mobile(), user.username(), passwordHash,
                user.role(), user.createdDate());
    }

    /** Updates the editable fields of an existing account. Password is changed separately. */
    public void update(User user) {
        database.update("""
                        UPDATE users SET name = ?, email = ?, mobile = ?, username = ?, role = ?
                        WHERE id = ?
                        """,
                user.name(), user.email(), user.mobile(), user.username(), user.role(), user.id());
    }

    public void updatePasswordHash(long userId, String passwordHash) {
        database.update("UPDATE users SET password_hash = ? WHERE id = ?", passwordHash, userId);
    }

    public void delete(long userId) {
        database.update("DELETE FROM users WHERE id = ?", userId);
    }

    /** Whether some other account already uses {@code username}. */
    public boolean usernameTaken(String username, long excludingUserId) {
        return database.count("SELECT COUNT(*) FROM users WHERE username = ? AND id <> ?",
                username, excludingUserId) > 0;
    }

    /** Whether some other account already uses {@code email}. */
    public boolean emailTaken(String email, long excludingUserId) {
        return database.count("SELECT COUNT(*) FROM users WHERE email = ? AND id <> ?",
                email, excludingUserId) > 0;
    }

    public int countAll() {
        return (int) database.count("SELECT COUNT(*) FROM users");
    }

    public int countByRole(Role role) {
        return (int) database.count("SELECT COUNT(*) FROM users WHERE role = ?", role);
    }

    /** Members ranked by how many books they have ever borrowed. */
    public List<RankedTitle> topBorrowers(int limit) {
        return database.query("""
                        SELECT u.name AS label, u.email AS sublabel, COUNT(l.id) AS loan_count
                        FROM users u
                        JOIN loans l ON l.user_id = u.id
                        GROUP BY u.id
                        ORDER BY loan_count DESC, u.name COLLATE NOCASE
                        LIMIT ?
                        """,
                rs -> new RankedTitle(rs.getString("label"), rs.getString("sublabel"),
                        rs.getInt("loan_count")),
                limit);
    }
}
