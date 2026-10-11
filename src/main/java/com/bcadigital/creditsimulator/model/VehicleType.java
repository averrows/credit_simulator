package com.bcadigital.creditsimulator.model;

import java.util.List;

/**
 * Vehicle kinds accepted by the simulator. Their interest rates come from a
 * {@link RatePolicyFactory}.
 *
 * <p>Parsing is lenient like menu commands: case-insensitive, trimmed, and a unique
 * prefix auto-matches (e.g. {@code "mot"} → Motor). A shared prefix such as
 * {@code "mo"} is rejected with a message naming both candidates.
 */
public enum VehicleType implements Choice {
    /** Car. */
    MOBIL("Mobil"),
    /** Motorcycle. */
    MOTOR("Motor");

    private static final ChoiceMatcher<VehicleType> MATCHER = new ChoiceMatcher<>(List.of(values()), "pilihan");

    private final String displayName;

    VehicleType(final String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the display name shown to users.
     *
     * @return the display name, e.g. {@code "Mobil"}
     */
    @Override
    public String displayName() {
        return displayName;
    }

    /**
     * Parses raw user input into a vehicle type, accepting a unique prefix.
     *
     * @param raw the raw input; may be null
     * @return the match ({@code assumed = true} for prefix matches), never null
     * @throws InvalidChoiceException when the input is null, blank, unknown, or ambiguous
     */
    public static Match<VehicleType> parse(final String raw) {
        return MATCHER.match(raw);
    }
}
