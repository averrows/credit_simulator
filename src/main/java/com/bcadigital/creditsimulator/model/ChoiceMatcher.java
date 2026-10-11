package com.bcadigital.creditsimulator.model;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Lenient parser from raw user input to one of a fixed set of {@link Choice}s.
 *
 * <p>Matching is case-insensitive and trims surrounding whitespace. An exact key
 * yields {@code assumed = false}; a unique prefix yields {@code assumed = true} so
 * callers can tell the user what was auto-selected. Everything else fails with an
 * {@link InvalidChoiceException} whose message already says whether the input was
 * unknown or ambiguous, so callers never inspect candidates themselves.
 *
 * @param <T> the option type
 */
public final class ChoiceMatcher<T extends Choice> {

    private final List<T> options;
    private final String noun;

    /**
     * Creates a matcher over the given options.
     *
     * @param options every option in display order; never null or empty
     * @param noun what an option is called in the "unknown" message, e.g.
     *     {@code "perintah"}; never null
     */
    public ChoiceMatcher(final List<T> options, final String noun) {
        this.options = List.copyOf(options);
        this.noun = Objects.requireNonNull(noun, "noun");
        if (this.options.isEmpty()) {
            throw new IllegalArgumentException("options must not be empty");
        }
    }

    /**
     * Matches raw input against the options.
     *
     * @param raw the raw input; may be null
     * @return the exact or uniquely-prefixed match, never null
     * @throws InvalidChoiceException when the input is null, blank, unknown, or a
     *     prefix of more than one option
     */
    public Match<T> match(final String raw) {
        final String typed = raw == null ? "" : raw.trim();
        final String normalized = typed.toLowerCase(Locale.ROOT);
        if (!normalized.isEmpty()) {
            for (final T option : options) {
                if (option.key().equals(normalized)) {
                    return new Match<>(option, false);
                }
            }
            final List<T> candidates = options.stream()
                    .filter(option -> option.key().startsWith(normalized))
                    .toList();
            if (candidates.size() == 1) {
                return new Match<>(candidates.get(0), true);
            }
            if (candidates.size() > 1) {
                throw new InvalidChoiceException("\"" + typed + "\" ambigu (" + displayNames(candidates) + "?)");
            }
        }
        throw new InvalidChoiceException(noun + " tidak dikenal: \"" + typed + "\"");
    }

    private static String displayNames(final List<? extends Choice> choices) {
        final StringJoiner joiner = new StringJoiner(" atau ");
        choices.forEach(choice -> joiner.add(choice.displayName()));
        return joiner.toString();
    }
}
