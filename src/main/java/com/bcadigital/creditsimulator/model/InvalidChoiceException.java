package com.bcadigital.creditsimulator.model;

/**
 * The input matches no {@link Choice}, or is a prefix of several. The message names
 * the candidates when ambiguous, e.g. {@code "mo" ambigu (Mobil atau Motor?)}.
 */
public final class InvalidChoiceException extends InvalidInputException {

    InvalidChoiceException(final String message) {
        super(message);
    }
}
