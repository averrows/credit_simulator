package com.bcadigital.creditsimulator.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Vehicle conditions accepted by the simulator.
 *
 * <p>Parsing is lenient: case-insensitive, trimmed, and a unique prefix auto-matches
 * (e.g. {@code "bek"} → Bekas). A shared prefix such as {@code "b"} matches nothing;
 * callers can use {@link #candidates(String)} to report ambiguity.
 */
public enum VehicleCondition {
    /** New vehicle; minimum down payment 35% and year at least currentYear - 1. */
    BARU("Baru"),
    /** Used vehicle; minimum down payment 25%. */
    BEKAS("Bekas");

    private final String displayName;

    VehicleCondition(final String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the display name shown to users.
     *
     * @return the display name, e.g. {@code "Bekas"}
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Returns the lowercase key used for matching user input.
     *
     * @return the lowercase key, e.g. {@code "bekas"}
     */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Parses raw user input into a condition, accepting a unique prefix.
     *
     * @param raw the raw input; may be null
     * @return the match ({@code assumed = true} for prefix matches), or empty when
     *     the input is null, blank, unknown, or ambiguous
     */
    public static Optional<Match<VehicleCondition>> parse(final String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        final String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (final VehicleCondition condition : values()) {
            if (condition.key().equals(normalized)) {
                return Optional.of(new Match<>(condition, false));
            }
        }
        final List<VehicleCondition> found = candidates(raw);
        return found.size() == 1 ? Optional.of(new Match<>(found.get(0), true)) : Optional.empty();
    }

    /**
     * Lists every condition whose key starts with the raw input.
     *
     * @param raw the raw input; may be null
     * @return the matching candidates, possibly empty, never null
     */
    public static List<VehicleCondition> candidates(final String raw) {
        final List<VehicleCondition> found = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return found;
        }
        final String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (final VehicleCondition condition : values()) {
            if (condition.key().startsWith(normalized)) {
                found.add(condition);
            }
        }
        return found;
    }
}
