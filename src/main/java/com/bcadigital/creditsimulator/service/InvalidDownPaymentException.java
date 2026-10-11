package com.bcadigital.creditsimulator.service;

/**
 * The down payment is not numeric, not less than the total loan, or below the
 * condition minimum: 35% for new vehicles, 25% for used ones (Rules 3-4).
 */
public final class InvalidDownPaymentException extends InvalidLoanInputException {

    /**
     * Creates the failure.
     *
     * @param message the user-facing reason; never null
     */
    public InvalidDownPaymentException(final String message) {
        super(message);
    }
}
