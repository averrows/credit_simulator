package com.bcadigital.creditsimulator.service;

/**
 * The vehicle year is not a 4-digit number, or a new vehicle is older than
 * {@code currentYear - 1} (Rule 1).
 */
public final class InvalidYearException extends InvalidLoanInputException {

    /**
     * Creates the failure.
     *
     * @param message the user-facing reason; never null
     */
    public InvalidYearException(final String message) {
        super(message);
    }
}
