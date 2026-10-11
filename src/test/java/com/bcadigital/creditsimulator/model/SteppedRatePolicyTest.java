package com.bcadigital.creditsimulator.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class SteppedRatePolicyTest {

    @Test
    void shouldStepAlternately_whenMobil() {
        final RatePolicy policy = VehicleType.MOBIL.ratePolicy();

        assertThat(policy.annualRate(1)).isEqualByComparingTo("0.08");
        assertThat(policy.annualRate(2)).isEqualByComparingTo("0.081");
        assertThat(policy.annualRate(3)).isEqualByComparingTo("0.086");
        assertThat(policy.annualRate(4)).isEqualByComparingTo("0.087");
        assertThat(policy.annualRate(5)).isEqualByComparingTo("0.092");
        assertThat(policy.annualRate(6)).isEqualByComparingTo("0.093");
    }

    @Test
    void shouldStartFromNinePercent_whenMotor() {
        final RatePolicy policy = VehicleType.MOTOR.ratePolicy();

        assertThat(policy.annualRate(1)).isEqualByComparingTo("0.09");
        assertThat(policy.annualRate(2)).isEqualByComparingTo("0.091");
        assertThat(policy.annualRate(3)).isEqualByComparingTo("0.096");
    }

    @Test
    void shouldReject_whenYearBelowOne() {
        final RatePolicy policy = VehicleType.MOBIL.ratePolicy();

        assertThrows(IllegalArgumentException.class, () -> policy.annualRate(0));
    }

    @Test
    void shouldUseExactDecimalArithmetic_whenStepping() {
        final RatePolicy policy = VehicleType.MOBIL.ratePolicy();

        assertThat(policy.annualRate(3)).isEqualTo(new BigDecimal("0.086"));
    }

    @Test
    void shouldStepFromAnyBase_whenConstructedDirectly() {
        final RatePolicy policy = new SteppedRatePolicy(new BigDecimal("0.10"));

        assertThat(policy.annualRate(3)).isEqualByComparingTo("0.106");
    }
}
