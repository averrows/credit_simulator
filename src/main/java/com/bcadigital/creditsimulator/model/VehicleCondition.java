package com.bcadigital.creditsimulator.model;

import java.util.List;

/**
 * Vehicle conditions accepted by the simulator.
 *
 * <p>Parsing is lenient: case-insensitive, trimmed, and a unique prefix auto-matches
 * (e.g. {@code "bek"} → Bekas). A shared prefix such as {@code "b"} is rejected
 * with a message naming both candidates.
 */
public enum VehicleCondition implements Choice {
    /** New vehicle; minimum down payment 35% and year at least currentYear - 1. */
    BARU("Baru"),
    /** Used vehicle; minimum down payment 25%. */
    BEKAS("Bekas");

    private static final ChoiceMatcher<VehicleCondition> MATCHER = new ChoiceMatcher<>(List.of(values()), "pilihan");

    private final String displayName;

    VehicleCondition(final String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the display name shown to users.
     *
     * @return the display name, e.g. {@code "Bekas"}
     */
    @Override
    public String displayName() {
        return displayName;
    }

    /**
     * Parses raw user input into a condition, accepting a unique prefix.
     *
     * @param raw the raw input; may be null
     * @return the match ({@code assumed = true} for prefix matches), never null
     * @throws InvalidChoiceException when the input is null, blank, unknown, or ambiguous
     */
    public static Match<VehicleCondition> parse(final String raw) {
        return MATCHER.match(raw);
    }
}
