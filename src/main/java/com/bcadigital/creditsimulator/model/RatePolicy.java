package com.bcadigital.creditsimulator.model;

import java.math.BigDecimal;

/**
 * Annual interest-rate schedule for one vehicle kind (Strategy pattern).
 *
 * <p>Each {@link VehicleType} owns one policy, so the calculator asks the vehicle
 * for its rates instead of branching on which vehicle it is.
 */
public interface RatePolicy {

    /**
     * Returns the annual rate for the given loan year.
     *
     * @param year the 1-based loan year
     * @return the rate as a fraction, e.g. {@code 0.08} for 8%; never null
     * @throws IllegalArgumentException when year is less than 1
     */
    BigDecimal annualRate(int year);
}
