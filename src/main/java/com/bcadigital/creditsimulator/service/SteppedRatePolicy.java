package com.bcadigital.creditsimulator.service;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Rate schedule starting from a base rate with alternating yearly steps.
 *
 * <p>Year 1 uses the base rate. Each following year adds 0,1% on even years and
 * 0,5% on odd years (8% → 8,1% → 8,6% → 8,7% …), which is the only reading of
 * Rules 7-8 consistent with the Rumus sample. Created via
 * {@link VehicleRateFactory}, never directly.
 */
final class SteppedRatePolicy implements RatePolicy {

    /** Step added on even years (0,1%). */
    private static final BigDecimal EVEN_YEAR_STEP = new BigDecimal("0.001");

    /** Step added on odd years after the first (0,5%). */
    private static final BigDecimal ODD_YEAR_STEP = new BigDecimal("0.005");

    private final BigDecimal base;

    SteppedRatePolicy(final BigDecimal base) {
        this.base = Objects.requireNonNull(base, "base");
    }

    @Override
    public BigDecimal annualRate(final int year) {
        if (year < 1) {
            throw new IllegalArgumentException("year harus >= 1, dapat: " + year);
        }
        BigDecimal rate = base;
        for (int current = 2; current <= year; current++) {
            rate = rate.add(current % 2 == 0 ? EVEN_YEAR_STEP : ODD_YEAR_STEP);
        }
        return rate;
    }
}
