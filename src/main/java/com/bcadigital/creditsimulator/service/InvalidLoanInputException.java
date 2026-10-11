package com.bcadigital.creditsimulator.service;

import com.bcadigital.creditsimulator.model.InvalidInputException;

/**
 * Base for all loan-input validation failures (Rules of the Game 1-4).
 *
 * <p>Each subclass names the offending input by its type, e.g.
 * {@link InvalidTenorException}, so callers that care which field failed catch
 * that subclass; callers that only report the problem catch this base.
 */
public abstract class InvalidLoanInputException extends InvalidInputException {

    /**
     * Creates a validation failure.
     *
     * @param message the user-facing reason; never null
     */
    protected InvalidLoanInputException(final String message) {
        super(message);
    }
}
