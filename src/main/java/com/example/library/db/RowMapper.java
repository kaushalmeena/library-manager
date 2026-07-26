package com.example.library.db;

import java.sql.ResultSet;
import java.sql.SQLException;

/** Turns the current row of a {@link ResultSet} into a domain object. */
@FunctionalInterface
public interface RowMapper<T> {

    T map(ResultSet rs) throws SQLException;
}
