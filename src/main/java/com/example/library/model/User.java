package com.example.library.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A library account.
 *
 * <p>Deliberately carries no password material; credentials are only ever read inside
 * {@link com.example.library.repository.UserRepository} while verifying a sign-in.
 *
 * @param id          database identity, {@code 0} for a record that has not been saved yet
 * @param name        the member's display name
 * @param email       unique email address, also usable as a sign-in handle
 * @param mobile      optional contact number
 * @param username    unique sign-in handle
 * @param role        what this account may do
 * @param createdDate the day the account was opened
 */
public record User(
        long id,
        String name,
        String email,
        String mobile,
        String username,
        Role role,
        LocalDate createdDate) {

    public User {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(role, "role");
    }

    /** A first initial pair used by the avatar chip, e.g. {@code AL} for "Ada Lovelace". */
    public String initials() {
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return "?";
        }
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase(java.util.Locale.ROOT);
        }
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0))
                .toUpperCase(java.util.Locale.ROOT);
    }
}
