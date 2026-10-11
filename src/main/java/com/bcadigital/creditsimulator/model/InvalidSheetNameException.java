package com.bcadigital.creditsimulator.model;

/**
 * A sheet name is blank, longer than 31 characters, or uses characters Excel
 * does not allow in tab names.
 */
public final class InvalidSheetNameException extends InvalidInputException {

    InvalidSheetNameException(final String message) {
        super(message);
    }
}
