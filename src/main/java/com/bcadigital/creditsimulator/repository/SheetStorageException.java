package com.bcadigital.creditsimulator.repository;

/**
 * Saved sheets cannot be read or written. The message is user-facing.
 *
 * <p>Unchecked: callers report it and carry on, since a storage problem must never
 * end the session or lose the calculation on screen.
 */
public final class SheetStorageException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the failure.
     *
     * @param message the user-facing reason; never null
     * @param cause the underlying failure; may be null
     */
    public SheetStorageException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
