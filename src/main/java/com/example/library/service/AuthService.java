package com.example.library.service;

import com.example.library.model.Role;
import com.example.library.model.User;
import com.example.library.repository.UserRepository;

import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/** Registration, sign-in and password changes. */
public final class AuthService {

    /** Cost factor for bcrypt; 10 keeps sign-in imperceptible while staying respectable. */
    private static final int BCRYPT_COST = 10;

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[\\w.-]{2,}$");
    private static final Pattern USERNAME = Pattern.compile("^[a-zA-Z0-9._-]{3,20}$");
    private static final Pattern MOBILE = Pattern.compile("^\\+?[0-9 -]{7,15}$");

    private final UserRepository users;

    public AuthService(UserRepository users) {
        this.users = Objects.requireNonNull(users, "users");
    }

    /**
     * Verifies a sign-in attempt.
     *
     * @param handle   username or email address
     * @param password the plaintext password as typed
     * @return the signed-in account, or empty when the handle is unknown or the password wrong
     */
    public Optional<User> authenticate(String handle, char[] password) {
        String normalised = normalise(handle);
        if (normalised.isEmpty() || password == null || password.length == 0) {
            return Optional.empty();
        }
        Optional<UserRepository.StoredCredentials> stored = users.findCredentials(normalised);
        if (stored.isEmpty()) {
            // Hash anyway so an unknown handle takes as long as a wrong password.
            BCrypt.checkpw(new String(password), dummyHash());
            return Optional.empty();
        }
        UserRepository.StoredCredentials credentials = stored.get();
        if (!BCrypt.checkpw(new String(password), credentials.passwordHash())) {
            return Optional.empty();
        }
        return users.findById(credentials.userId());
    }

    /**
     * Creates an account after checking the details are well formed and unused.
     *
     * @return the newly created account
     * @throws ValidationException when any field is invalid or already taken
     */
    public User register(String name, String email, String mobile, String username,
                        char[] password, Role role) {
        String cleanName = trim(name);
        String cleanEmail = normalise(email);
        String cleanMobile = trim(mobile);
        String cleanUsername = normalise(username);

        List<String> problems = new ArrayList<>();
        if (cleanName.length() < 2) {
            problems.add("Name must be at least 2 characters.");
        }
        if (!EMAIL.matcher(cleanEmail).matches()) {
            problems.add("Email address is not valid.");
        }
        if (!cleanMobile.isEmpty() && !MOBILE.matcher(cleanMobile).matches()) {
            problems.add("Mobile number is not valid.");
        }
        if (!USERNAME.matcher(cleanUsername).matches()) {
            problems.add("Username must be 3-20 characters, letters, digits, dot, dash "
                    + "or underscore.");
        }
        problems.addAll(passwordProblems(password));
        if (problems.isEmpty()) {
            if (users.usernameTaken(cleanUsername, 0)) {
                problems.add("That username is already taken.");
            }
            if (users.emailTaken(cleanEmail, 0)) {
                problems.add("That email address is already registered.");
            }
        }
        if (!problems.isEmpty()) {
            throw new ValidationException(problems);
        }

        User draft = new User(0, cleanName, cleanEmail, emptyToNull(cleanMobile), cleanUsername,
                role == null ? Role.STUDENT : role, LocalDate.now());
        long id = users.insert(draft, hash(password));
        return users.findById(id).orElseThrow(
                () -> new IllegalStateException("Account " + id + " vanished after insert"));
    }

    /**
     * Updates the editable details of an existing account.
     *
     * @throws ValidationException when any field is invalid or taken by another account
     */
    public User updateProfile(long userId, String name, String email, String mobile,
                             String username, Role role) {
        String cleanName = trim(name);
        String cleanEmail = normalise(email);
        String cleanMobile = trim(mobile);
        String cleanUsername = normalise(username);

        List<String> problems = new ArrayList<>();
        if (cleanName.length() < 2) {
            problems.add("Name must be at least 2 characters.");
        }
        if (!EMAIL.matcher(cleanEmail).matches()) {
            problems.add("Email address is not valid.");
        }
        if (!cleanMobile.isEmpty() && !MOBILE.matcher(cleanMobile).matches()) {
            problems.add("Mobile number is not valid.");
        }
        if (!USERNAME.matcher(cleanUsername).matches()) {
            problems.add("Username must be 3-20 characters, letters, digits, dot, dash "
                    + "or underscore.");
        }
        if (problems.isEmpty()) {
            if (users.usernameTaken(cleanUsername, userId)) {
                problems.add("That username is already taken.");
            }
            if (users.emailTaken(cleanEmail, userId)) {
                problems.add("That email address belongs to another account.");
            }
        }
        if (!problems.isEmpty()) {
            throw new ValidationException(problems);
        }

        User existing = users.findById(userId).orElseThrow(
                () -> new ValidationException("That account no longer exists."));
        User updated = new User(userId, cleanName, cleanEmail, emptyToNull(cleanMobile),
                cleanUsername, role == null ? existing.role() : role, existing.createdDate());
        users.update(updated);
        return updated;
    }

    /**
     * Changes a password after confirming the current one.
     *
     * @throws ValidationException when the current password is wrong or the new one is weak
     */
    public void changePassword(User user, char[] currentPassword, char[] newPassword,
                               char[] confirmation) {
        if (authenticate(user.username(), currentPassword).isEmpty()) {
            throw new ValidationException("Current password is incorrect.");
        }
        List<String> problems = new ArrayList<>(passwordProblems(newPassword));
        if (!java.util.Arrays.equals(newPassword, confirmation)) {
            problems.add("New password and confirmation do not match.");
        }
        if (!problems.isEmpty()) {
            throw new ValidationException(problems);
        }
        users.updatePasswordHash(user.id(), hash(newPassword));
    }

    /**
     * Resets a password without knowing the old one. Only reachable from account management.
     *
     * @throws ValidationException when the new password is too weak
     */
    public void resetPassword(long userId, char[] newPassword) {
        List<String> problems = passwordProblems(newPassword);
        if (!problems.isEmpty()) {
            throw new ValidationException(problems);
        }
        users.updatePasswordHash(userId, hash(newPassword));
    }

    /**
     * Deletes an account.
     *
     * @throws ValidationException when deleting would lock everyone out or is self-inflicted
     */
    public void deleteAccount(User actor, long targetUserId) {
        if (actor.id() == targetUserId) {
            throw new ValidationException("You cannot delete your own account.");
        }
        User target = users.findById(targetUserId).orElseThrow(
                () -> new ValidationException("That account no longer exists."));
        if (target.role() == Role.ADMIN && users.countByRole(Role.ADMIN) <= 1) {
            throw new ValidationException("The last remaining admin account cannot be deleted.");
        }
        users.delete(targetUserId);
    }

    private static List<String> passwordProblems(char[] password) {
        List<String> problems = new ArrayList<>();
        if (password == null || password.length < MIN_PASSWORD_LENGTH) {
            problems.add("Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
            return problems;
        }
        String value = new String(password);
        if (value.chars().noneMatch(Character::isLetter)) {
            problems.add("Password must contain at least one letter.");
        }
        if (value.chars().noneMatch(Character::isDigit)) {
            problems.add("Password must contain at least one digit.");
        }
        return problems;
    }

    private static String hash(char[] password) {
        return BCrypt.hashpw(new String(password), BCrypt.gensalt(BCRYPT_COST));
    }

    /** A throw-away hash used purely to equalise timing for unknown accounts. */
    private static String dummyHash() {
        return "$2a$10$Js3ceMbrMDyxsG3GJJbdiOCTNIe.Jcy1.j.WId4TxQxuXBnmQofuq";
    }

    private static String normalise(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
