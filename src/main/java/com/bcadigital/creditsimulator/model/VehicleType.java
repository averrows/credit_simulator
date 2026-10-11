package com.bcadigital.creditsimulator.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Vehicle kinds accepted by the simulator, each owning its interest-rate
 * {@link RatePolicy} (Rule 9: Mobil starts at 8%, Motor at 9%).
 *
 * <p>Parsing is lenient like menu commands: case-insensitive, trimmed, and a unique
 * prefix auto-matches (e.g. {@code "mot"} → Motor). A shared prefix such as
 * {@code "mo"} is rejected with a message naming both candidates.
 */
public enum VehicleType implements Choice {
    /** Car; base interest rate 8%. */
    MOBIL("Mobil", new SteppedRatePolicy(new BigDecimal("0.08"))),
    /** Motorcycle; base interest rate 9%. */
    MOTOR("Motor", new SteppedRatePolicy(new BigDecimal("0.09")));

    private static final ChoiceMatcher<VehicleType> MATCHER = new ChoiceMatcher<>(List.of(values()), "pilihan");

    private final String displayName;
    private final RatePolicy ratePolicy;

    VehicleType(final String displayName, final RatePolicy ratePolicy) {
        this.displayName = displayName;
        this.ratePolicy = ratePolicy;
    }

    /**
     * Returns the interest-rate schedule for loans on this vehicle kind.
     *
     * @return the policy, never null; stateless and safe to share
     */
    public RatePolicy ratePolicy() {
        return ratePolicy;
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
