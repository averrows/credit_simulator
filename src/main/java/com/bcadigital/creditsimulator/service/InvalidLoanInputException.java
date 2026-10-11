package com.bcadigital.creditsimulator.service;

import java.util.Objects;

import com.bcadigital.creditsimulator.model.InvalidInputException;

/**
 * Base for all loan-input validation failures (Rules of the Game 1-4).
 *
 * <p>Unchecked: invalid input is an expected user error that the controller
 * translates into a friendly message and a re-prompt, never a crash. Each failure
 * carries the offending {@link Field} so the controller can re-ask exactly that
 * input instead of restarting the whole form.
 */
public class InvalidLoanInputException extends InvalidInputException {

    /** Which input failed validation. */
    public enum Field {
        /** Vehicle year. */
        VEHICLE_YEAR,
        /** Total loan amount. */
        TOTAL_LOAN,
        /** Loan tenure. */
        TENURE,
        /** Down payment. */
        DOWN_PAYMENT
    }

    private final Field field;

    /**
     * Creates a validation failure.
     *
     * @param field the offending input; never null
     * @param message the user-facing reason; never null
     */
    protected InvalidLoanInputException(final Field field, final String message) {
        super(message);
        this.field = Objects.requireNonNull(field, "field");
    }

    /**
     * Returns which input failed validation.
     *
     * @return the offending field, never null
     */
    public Field getField() {
        return field;
    }
}
