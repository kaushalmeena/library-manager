package com.example.library.model;

/** What a signed-in account is allowed to do. */
public enum Role {

    /** Manages accounts and sees library-wide reporting. */
    ADMIN("Admin"),

    /** Runs the desk: catalogue upkeep plus issuing and returning books. */
    LIBRARIAN("Librarian"),

    /** Borrows books and tracks their own loans and fines. */
    STUDENT("Student");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /** Whether the role may create, edit and delete accounts. */
    public boolean canManageUsers() {
        return this == ADMIN;
    }

    /** Whether the role may add, edit and delete catalogue entries. */
    public boolean canManageBooks() {
        return this == ADMIN || this == LIBRARIAN;
    }

    /** Whether the role may issue and return books on behalf of members. */
    public boolean canCirculate() {
        return this == ADMIN || this == LIBRARIAN;
    }

    /** Whether the role sees every loan rather than only its own. */
    public boolean canSeeAllLoans() {
        return this == ADMIN || this == LIBRARIAN;
    }

    /** Parses a stored role, defaulting to {@link #STUDENT} for unknown values. */
    public static Role fromDatabase(String value) {
        if (value == null) {
            return STUDENT;
        }
        try {
            return Role.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return STUDENT;
        }
    }
}
