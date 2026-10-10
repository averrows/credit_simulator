package com.bcadigital.creditsimulator.service;

import java.math.BigDecimal;

/**
 * Annual interest-rate schedule for one vehicle kind.
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
