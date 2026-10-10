package com.bcadigital.creditsimulator.service;

/**
 * The loan tenure is not numeric or outside 1-6 years (Rule 2).
 */
public final class InvalidTenorException extends InvalidLoanInputException {

    /**
     * Creates the failure.
     *
     * @param message the user-facing reason; never null
     */
    public InvalidTenorException(final String message) {
        super(Field.TENURE, message);
    }
}
