package com.example.library.db;

/** Wraps the checked {@link java.sql.SQLException} so callers are not forced to handle JDBC. */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(String message) {
        super(message);
    }
}
