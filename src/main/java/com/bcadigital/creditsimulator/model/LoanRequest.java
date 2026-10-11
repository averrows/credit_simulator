package com.bcadigital.creditsimulator.model;

import java.util.Objects;

/**
 * A vehicle-loan request, ready for calculation.
 *
 * <p>The record performs no rule checks itself: input rules live in
 * {@code LoanValidator} and per-condition limits in {@link VehicleCondition}.
 * Constructing this record directly (e.g. in tests) bypasses validation.
 *
 * @param vehicleType the vehicle kind; never null
 * @param vehicleCondition the vehicle condition; never null
 * @param vehicleYear the 4-digit vehicle year
 * @param totalLoanAmount the total loan amount in rupiah, 1 to 1.000.000.000
 * @param loanTenureYears the tenure in years, 1 to 6
 * @param downPayment the down payment in rupiah, at least the condition minimum
 *     and less than the total loan amount
 */
public record LoanRequest(
        VehicleType vehicleType,
        VehicleCondition vehicleCondition,
        int vehicleYear,
        long totalLoanAmount,
        int loanTenureYears,
        long downPayment) {

    /**
     * Creates a loan request without validating the amounts.
     */
    public LoanRequest {
        Objects.requireNonNull(vehicleType, "vehicleType");
        Objects.requireNonNull(vehicleCondition, "vehicleCondition");
    }

    /**
     * Returns the amount actually financed.
     *
     * @return the total loan amount minus the down payment, in rupiah
     */
    public long principal() {
        return totalLoanAmount - downPayment;
    }
}
