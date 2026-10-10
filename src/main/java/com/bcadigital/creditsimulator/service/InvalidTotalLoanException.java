package com.bcadigital.creditsimulator.service;

/**
 * The total loan amount is not numeric or exceeds 1 miliar.
 */
public final class InvalidTotalLoanException extends InvalidLoanInputException {

    /**
     * Creates the failure.
     *
     * @param message the user-facing reason; never null
     */
    public InvalidTotalLoanException(final String message) {
        super(Field.TOTAL_LOAN, message);
    }
}
