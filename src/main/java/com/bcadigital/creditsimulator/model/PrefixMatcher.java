package com.bcadigital.creditsimulator.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Lenient prefix matching shared by all user-facing enums.
 *
 * <p>Matching is case-insensitive and trims surrounding whitespace. An exact key
 * yields {@code assumed = false}; a unique prefix yields {@code assumed = true}
 * so callers can tell the user what was auto-selected. Unknown, blank, or
 * ambiguous input yields empty instead of an exception: a mistyped choice is an
 * expected user error, not a programming failure.
 */
public final class PrefixMatcher {

    private PrefixMatcher() {
    }

    /**
     * Parses raw input against the given values, accepting a unique prefix.
     *
     * @param <T> the option type
     * @param values every candidate in declaration order; never null, no null elements
     * @param keyOf lowercase match key of one value; never null
     * @param raw the raw input; may be null
     * @return the match, or empty when the input is null, blank, unknown, or ambiguous
     */
    public static <T> Optional<Match<T>> parse(
            final List<T> values, final Function<T, String> keyOf, final String raw) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(keyOf, "keyOf");
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        final String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (final T value : values) {
            if (keyOf.apply(value).equals(normalized)) {
                return Optional.of(new Match<>(value, false));
            }
        }
        final List<T> found = candidates(values, keyOf, raw);
        return found.size() == 1 ? Optional.of(new Match<>(found.get(0), true)) : Optional.empty();
    }

    /**
     * Lists every value whose key starts with the raw input.
     *
     * <p>Callers use this to distinguish "ambiguous" (several candidates) from
     * "unknown" (none) in error messages.
     *
     * @param <T> the option type
     * @param values every candidate; never null
     * @param keyOf lowercase match key of one value; never null
     * @param raw the raw input; may be null
     * @return the matching candidates, possibly empty, never null
     */
    public static <T> List<T> candidates(
            final List<T> values, final Function<T, String> keyOf, final String raw) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(keyOf, "keyOf");
        final List<T> found = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return found;
        }
        final String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (final T value : values) {
            if (keyOf.apply(value).startsWith(normalized)) {
                found.add(value);
            }
        }
        return found;
    }
}
