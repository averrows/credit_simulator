package com.bcadigital.creditsimulator.model;

import java.util.List;
import java.util.OptionalInt;

/**
 * Vehicle conditions accepted by the simulator, each owning its loan rules
 * (Rules 1, 3-4): the oldest acceptable model year and the minimum down payment.
 * Validators ask the condition instead of branching on which one it is.
 *
 * <p>Parsing is lenient: case-insensitive, trimmed, and a unique prefix auto-matches
 * (e.g. {@code "bek"} → Bekas). A shared prefix such as {@code "b"} is rejected
 * with a message naming both candidates.
 */
public enum VehicleCondition implements Choice {
    /** New vehicle: model year at least last year, down payment at least 35%. */
    BARU("Baru", 35) {
        @Override
        public OptionalInt minimumYear(final int currentYear) {
            return OptionalInt.of(currentYear - 1);
        }
    },
    /** Used vehicle: any model year, down payment at least 25%. */
    BEKAS("Bekas", 25) {
        @Override
        public OptionalInt minimumYear(final int currentYear) {
            return OptionalInt.empty();
        }
    };

    private static final ChoiceMatcher<VehicleCondition> MATCHER = new ChoiceMatcher<>(List.of(values()), "pilihan");

    private final String displayName;
    private final int minDownPaymentPercent;

    VehicleCondition(final String displayName, final int minDownPaymentPercent) {
        this.displayName = displayName;
        this.minDownPaymentPercent = minDownPaymentPercent;
    }

    /**
     * Returns the oldest model year a loan accepts for this condition.
     *
     * @param currentYear the calendar year the loan is requested in
     * @return the minimum year, or empty when any year is accepted
     */
    public abstract OptionalInt minimumYear(int currentYear);

    /**
     * Returns the smallest down payment allowed, as a percentage of the total loan.
     *
     * @return the percentage, e.g. {@code 35}
     */
    public int minDownPaymentPercent() {
        return minDownPaymentPercent;
    }

    /**
     * Tells whether a down payment meets this condition's minimum percentage.
     *
     * @param downPayment the down payment in rupiah
     * @param totalLoanAmount the total loan amount in rupiah
     * @return true when {@code downPayment} is at least the minimum share of the total
     */
    public boolean meetsMinimumDownPayment(final long downPayment, final long totalLoanAmount) {
        return downPayment * 100 >= (long) minDownPaymentPercent * totalLoanAmount;
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
