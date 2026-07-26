package com.example.library.service;

import com.example.library.model.Role;
import com.example.library.model.User;
import com.example.library.support.TestLibrary;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    private TestLibrary library;

    @BeforeEach
    void setUp() {
        library = TestLibrary.empty();
    }

    @AfterEach
    void tearDown() {
        library.close();
    }

    @Test
    @DisplayName("stores the password as a bcrypt hash, never as plain text")
    void hashesPassword() {
        User created = library.addMember("alan");

        String stored = library.users.findCredentials("alan").orElseThrow().passwordHash();
        assertTrue(stored.startsWith("$2a$"), "expected a bcrypt hash but got " + stored);
        assertNotEquals("password123", stored);
    }

    @Test
    @DisplayName("salts each hash, so identical passwords are stored differently")
    void saltsEachHash() {
        library.addMember("alan");
        library.addMember("grace");

        String first = library.users.findCredentials("alan").orElseThrow().passwordHash();
        String second = library.users.findCredentials("grace").orElseThrow().passwordHash();
        assertNotEquals(first, second);
    }

    @Test
    @DisplayName("accepts the right password by username or by email")
    void authenticatesByUsernameOrEmail() {
        User created = library.addMember("alan");

        assertTrue(library.auth.authenticate("alan", "password123".toCharArray()).isPresent());
        assertTrue(library.auth.authenticate(created.email(), "password123".toCharArray())
                .isPresent());
    }

    @Test
    @DisplayName("rejects a wrong password and an unknown account alike")
    void rejectsBadCredentials() {
        library.addMember("alan");

        assertTrue(library.auth.authenticate("alan", "wrong-password".toCharArray()).isEmpty());
        assertTrue(library.auth.authenticate("nobody", "password123".toCharArray()).isEmpty());
        assertTrue(library.auth.authenticate("alan", new char[0]).isEmpty());
        assertTrue(library.auth.authenticate(null, "password123".toCharArray()).isEmpty());
    }

    @Test
    @DisplayName("normalises the username and email to lower case")
    void normalisesHandles() {
        library.auth.register("Ada Lovelace", "Ada@Library.TEST", "9876543210", "AdaL",
                "password123".toCharArray(), Role.ADMIN);

        Optional<User> found = library.users.findByHandle("adal");
        assertTrue(found.isPresent());
        assertEquals("ada@library.test", found.get().email());
        assertEquals("adal", found.get().username());
    }

    @Test
    @DisplayName("refuses a duplicate username or email")
    void refusesDuplicates() {
        library.addMember("alan");

        ValidationException duplicateUsername = assertThrows(ValidationException.class,
                () -> library.auth.register("Someone Else", "other@library.test", "9876543210",
                        "alan", "password123".toCharArray(), Role.STUDENT));
        assertTrue(duplicateUsername.getMessage().contains("username"),
                duplicateUsername.getMessage());

        ValidationException duplicateEmail = assertThrows(ValidationException.class,
                () -> library.auth.register("Someone Else", "alan@library.test", "9876543210",
                        "someone", "password123".toCharArray(), Role.STUDENT));
        assertTrue(duplicateEmail.getMessage().contains("email"), duplicateEmail.getMessage());
    }

    @Test
    @DisplayName("reports every problem with a registration at once")
    void reportsAllProblems() {
        ValidationException thrown = assertThrows(ValidationException.class,
                () -> library.auth.register("A", "not-an-email", "abc", "x",
                        "short".toCharArray(), Role.STUDENT));

        assertTrue(thrown.problems().size() >= 4,
                "expected several problems but got " + thrown.problems());
    }

    @Test
    @DisplayName("requires a password of at least eight characters with a letter and a digit")
    void enforcesPasswordStrength() {
        assertThrows(ValidationException.class, () -> register("shrt1"));
        assertThrows(ValidationException.class, () -> register("alllettersnodigits"));
        assertThrows(ValidationException.class, () -> register("12345678"));

        User created = register("goodpass1");
        assertEquals("candidate", created.username());
    }

    private User register(String password) {
        return library.auth.register("Test Candidate", "candidate@library.test", "9876543210",
                "candidate", password.toCharArray(), Role.STUDENT);
    }

    @Test
    @DisplayName("changes a password only when the current one is right")
    void changesPassword() {
        User member = library.addMember("alan");

        assertThrows(ValidationException.class, () -> library.auth.changePassword(member,
                "wrong".toCharArray(), "newpassword1".toCharArray(),
                "newpassword1".toCharArray()));

        assertThrows(ValidationException.class, () -> library.auth.changePassword(member,
                "password123".toCharArray(), "newpassword1".toCharArray(),
                "different1".toCharArray()));

        library.auth.changePassword(member, "password123".toCharArray(),
                "newpassword1".toCharArray(), "newpassword1".toCharArray());

        assertTrue(library.auth.authenticate("alan", "newpassword1".toCharArray()).isPresent());
        assertFalse(library.auth.authenticate("alan", "password123".toCharArray()).isPresent());
    }

    @Test
    @DisplayName("updates a profile without disturbing the password")
    void updatesProfile() {
        User member = library.addMember("alan");

        User updated = library.auth.updateProfile(member.id(), "Alan M Turing",
                "alan.turing@library.test", "9998887776", "alan", Role.STUDENT);

        assertEquals("Alan M Turing", updated.name());
        assertEquals("alan.turing@library.test", updated.email());
        assertTrue(library.auth.authenticate("alan", "password123".toCharArray()).isPresent());
    }

    @Test
    @DisplayName("refuses to take an email already used by another account")
    void refusesTakenEmailOnUpdate() {
        User first = library.addMember("alan");
        library.addMember("grace");

        assertThrows(ValidationException.class, () -> library.auth.updateProfile(first.id(),
                "Alan Turing", "grace@library.test", "9876543210", "alan", Role.STUDENT));
    }

    @Test
    @DisplayName("refuses to delete your own account or the last admin")
    void protectsAdminAndSelf() {
        User admin = library.auth.register("Ada Lovelace", "ada@library.test", "9876543210",
                "ada", "password123".toCharArray(), Role.ADMIN);
        User student = library.addMember("alan");

        assertThrows(ValidationException.class,
                () -> library.auth.deleteAccount(admin, admin.id()));

        ValidationException lastAdmin = assertThrows(ValidationException.class,
                () -> library.auth.deleteAccount(student, admin.id()));
        assertTrue(lastAdmin.getMessage().contains("last remaining admin"),
                lastAdmin.getMessage());

        library.auth.deleteAccount(admin, student.id());
        assertTrue(library.users.findById(student.id()).isEmpty());
    }
}
