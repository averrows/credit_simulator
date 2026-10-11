package com.bcadigital.creditsimulator.model;

import java.util.Objects;

/**
 * Result of leniently parsing raw user input into a known {@link Choice}.
 *
 * <p>Parsing accepts an exact name or a unique prefix (e.g. {@code "mot"} for
 * {@code Motor}). The {@code assumed} flag tells the caller whether the value was
 * auto-selected from a prefix, so it can inform the user instead of silently guessing.
 *
 * @param <T> the type of the parsed value
 * @param value the matched value; never null
 * @param assumed true when matched by unique prefix rather than exact name
 */
public record Match<T extends Choice>(T value, boolean assumed) {

    /**
     * Creates a match.
     *
     * @param value the matched value; never null
     * @param assumed true when matched by unique prefix rather than exact name
     */
    public Match {
        Objects.requireNonNull(value, "value");
    }
}
