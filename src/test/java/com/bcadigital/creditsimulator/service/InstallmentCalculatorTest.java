package com.bcadigital.creditsimulator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.bcadigital.creditsimulator.model.LoanRequest;
import com.bcadigital.creditsimulator.model.SimulationResult;
import com.bcadigital.creditsimulator.model.VehicleCondition;
import com.bcadigital.creditsimulator.model.VehicleType;

class InstallmentCalculatorTest {

    private final InstallmentCalculator calculator = new InstallmentCalculator();

    private LoanRequest rumusCase() {
        return new LoanRequest(VehicleType.MOBIL, VehicleCondition.BEKAS, 2015, 100_000_000L, 3, 25_000_000L);
    }

    @Test
    void shouldFollowRumusSchedule_whenThreeYearMobil() {
        final SimulationResult result = calculator.calculate(rumusCase());

        assertThat(result.yearlyResults()).hasSize(3);
        assertThat(result.yearlyResults().get(0).year()).isEqualTo(1);
        assertThat(result.yearlyResults().get(0).annualRate()).isEqualByComparingTo("0.08");
        assertThat(result.yearlyResults().get(0).monthlyInstallment()).isEqualByComparingTo("2250000.00");
        assertThat(result.yearlyResults().get(1).annualRate()).isEqualByComparingTo("0.081");
        assertThat(result.yearlyResults().get(1).monthlyInstallment()).isEqualByComparingTo("2432250.00");
        assertThat(result.yearlyResults().get(2).annualRate()).isEqualByComparingTo("0.086");
        assertThat(result.yearlyResults().get(2).monthlyInstallment()).isEqualByComparingTo("2641423.50");
    }

    @Test
    void shouldSpreadOverTwelveMonths_whenTenorOneYear() {
        final LoanRequest request =
                new LoanRequest(VehicleType.MOTOR, VehicleCondition.BARU, 2025, 120_000_000L, 1, 42_000_000L);

        final SimulationResult result = calculator.calculate(request);

        assertThat(result.yearlyResults()).hasSize(1);
        assertThat(result.yearlyResults().get(0).monthlyInstallment()).isEqualByComparingTo("7085000.00");
    }

    @Test
    void shouldReject_whenRequestNull() {
        assertThrows(NullPointerException.class, () -> calculator.calculate(null));
    }
}
