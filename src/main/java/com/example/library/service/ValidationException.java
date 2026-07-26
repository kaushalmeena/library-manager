package com.example.library.service;

import java.util.List;

/**
 * Raised when input or a library rule prevents an operation. Carries one message per problem
 * so a form can show every complaint at once instead of one at a time.
 */
public class ValidationException extends RuntimeException {

    private final List<String> problems;

    public ValidationException(String problem) {
        this(List.of(problem));
    }

    public ValidationException(List<String> problems) {
        super(String.join("\n", problems));
        this.problems = List.copyOf(problems);
    }

    /** The individual complaints, suitable for a bulleted list. */
    public List<String> problems() {
        return problems;
    }
}
