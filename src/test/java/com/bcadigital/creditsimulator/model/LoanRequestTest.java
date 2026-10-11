package com.bcadigital.creditsimulator.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LoanRequestTest {

    @Test
    void shouldFinanceTotalMinusDownPayment_whenPrincipal() {
        final LoanRequest request = new LoanRequest(
                VehicleType.MOBIL, VehicleCondition.BARU, 2025, Rupiah.of(100_000_000L), 3, Rupiah.of(35_000_000L));

        assertThat(request.principal()).isEqualTo(Rupiah.of(65_000_000L));
    }
}
