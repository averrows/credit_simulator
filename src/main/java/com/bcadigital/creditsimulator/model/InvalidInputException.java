package com.bcadigital.creditsimulator.model;

import java.util.Objects;

/**
 * Base for every expected user-input error: an unknown choice or a rule-breaking value.
 *
 * <p>Unchecked because bad input is an expected event, not a programming failure.
 * The message is always user-facing, so callers show it as-is and re-prompt (or
 * abort in batch mode) without inspecting the concrete type.
 */
public abstract class InvalidInputException extends RuntimeException {

    /**
     * Creates an input error.
     *
     * @param message the user-facing reason; never null
     */
    protected InvalidInputException(final String message) {
        super(Objects.requireNonNull(message, "message"));
    }
}
