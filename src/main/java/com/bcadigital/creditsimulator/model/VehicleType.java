package com.bcadigital.creditsimulator.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Vehicle kinds accepted by the simulator.
 *
 * <p>Parsing is lenient like menu commands: case-insensitive, trimmed, and a unique
 * prefix auto-matches (e.g. {@code "mot"} → Motor). A shared prefix such as
 * {@code "mo"} matches nothing; callers can use {@link #candidates(String)} to tell
 * the user it was ambiguous instead of unknown.
 */
public enum VehicleType {
    /** Car; base interest rate 8% (applied by the rate policy, not here). */
    MOBIL("Mobil"),
    /** Motorcycle; base interest rate 9% (applied by the rate policy, not here). */
    MOTOR("Motor");

    private final String displayName;

    VehicleType(final String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the display name shown to users.
     *
     * @return the display name, e.g. {@code "Mobil"}
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Returns the lowercase key used for matching user input.
     *
     * @return the lowercase key, e.g. {@code "mobil"}
     */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Parses raw user input into a vehicle type, accepting a unique prefix.
     *
     * @param raw the raw input; may be null
     * @return the match ({@code assumed = true} for prefix matches), or empty when
     *     the input is null, blank, unknown, or ambiguous
     */
    public static Optional<Match<VehicleType>> parse(final String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        final String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (final VehicleType type : values()) {
            if (type.key().equals(normalized)) {
                return Optional.of(new Match<>(type, false));
            }
        }
        final List<VehicleType> found = candidates(raw);
        return found.size() == 1 ? Optional.of(new Match<>(found.get(0), true)) : Optional.empty();
    }

    /**
     * Lists every vehicle type whose key starts with the raw input.
     *
     * <p>Used to distinguish "ambiguous" (several candidates) from "unknown" (none)
     * in error messages.
     *
     * @param raw the raw input; may be null
     * @return the matching candidates, possibly empty, never null
     */
    public static List<VehicleType> candidates(final String raw) {
        final List<VehicleType> found = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return found;
        }
        final String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (final VehicleType type : values()) {
            if (type.key().startsWith(normalized)) {
                found.add(type);
            }
        }
        return found;
    }
}
