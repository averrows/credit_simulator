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
 * @param totalLoanAmount the total loan amount, 1 to 1.000.000.000 rupiah; never null
 * @param loanTenureYears the tenure in years, 1 to 6
 * @param downPayment the down payment, at least the condition minimum and less
 *     than the total loan amount; never null
 */
public record LoanRequest(
        VehicleType vehicleType,
        VehicleCondition vehicleCondition,
        int vehicleYear,
        Rupiah totalLoanAmount,
        int loanTenureYears,
        Rupiah downPayment) {

    /**
     * Creates a loan request without validating the amounts.
     */
    public LoanRequest {
        Objects.requireNonNull(vehicleType, "vehicleType");
        Objects.requireNonNull(vehicleCondition, "vehicleCondition");
        Objects.requireNonNull(totalLoanAmount, "totalLoanAmount");
        Objects.requireNonNull(downPayment, "downPayment");
        if (downPayment.isGreaterThan(totalLoanAmount)) {
            throw new IllegalArgumentException("downPayment exceeds totalLoanAmount");
        }
    }

    /**
     * Returns the amount actually financed.
     *
     * @return the total loan amount minus the down payment, never null
     */
    public Rupiah principal() {
        return totalLoanAmount.minus(downPayment);
    }
}
