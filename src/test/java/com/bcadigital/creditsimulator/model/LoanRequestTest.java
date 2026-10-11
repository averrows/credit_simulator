package com.bcadigital.creditsimulator.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LoanRequestTest {

    @Test
    void shouldFinanceTotalMinusDownPayment_whenPrincipal() {
        final LoanRequest request = new LoanRequest(
                VehicleType.MOBIL, VehicleCondition.BARU, 2025, 100_000_000L, 3, 35_000_000L);

        assertThat(request.principal()).isEqualTo(65_000_000L);
    }
}
