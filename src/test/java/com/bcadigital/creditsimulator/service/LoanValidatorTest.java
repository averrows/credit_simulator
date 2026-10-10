package com.bcadigital.creditsimulator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.bcadigital.creditsimulator.model.LoanRequest;
import com.bcadigital.creditsimulator.model.VehicleCondition;
import com.bcadigital.creditsimulator.model.VehicleType;
import com.bcadigital.creditsimulator.service.InvalidLoanInputException.Field;

class LoanValidatorTest {

    // Pinned to 2026, so "currentYear - 1" is 2025 for new vehicles.
    private final LoanValidator validator = new LoanValidator(
            Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC));

    private LoanRequest validBaru() {
        return validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "2025", "100000000", "3", "35000000");
    }

    @Test
    void shouldBuildRequest_whenAllInputsValid() {
        final LoanRequest request = validBaru();

        assertThat(request.vehicleType()).isEqualTo(VehicleType.MOBIL);
        assertThat(request.vehicleCondition()).isEqualTo(VehicleCondition.BARU);
        assertThat(request.vehicleYear()).isEqualTo(2025);
        assertThat(request.totalLoanAmount()).isEqualTo(100_000_000L);
        assertThat(request.loanTenureYears()).isEqualTo(3);
        assertThat(request.downPayment()).isEqualTo(35_000_000L);
    }

    @Test
    void shouldAccept_whenNewVehicleYearIsCurrentMinusOne() {
        final LoanRequest request = validator.validate(
                VehicleType.MOTOR, VehicleCondition.BARU, "2025", "50000000", "1", "20000000");

        assertThat(request.vehicleYear()).isEqualTo(2025);
    }

    @Test
    void shouldReject_whenNewVehicleYearTooOld() {
        final InvalidYearException failure = assertThrows(InvalidYearException.class, () -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "2024", "100000000", "3", "35000000"));

        assertThat(failure.getField()).isEqualTo(Field.VEHICLE_YEAR);
    }

    @Test
    void shouldAccept_whenUsedVehicleHasAnyFourDigitYear() {
        final LoanRequest request = validator.validate(
                VehicleType.MOBIL, VehicleCondition.BEKAS, "1990", "100000000", "3", "25000000");

        assertThat(request.vehicleYear()).isEqualTo(1990);
    }

    @Test
    void shouldReject_whenYearIsNotFourDigits() {
        assertThat(fieldOf(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "25", "100000000", "3", "35000000")))
                .isEqualTo(Field.VEHICLE_YEAR);
        assertThat(fieldOf(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "20245", "100000000", "3", "35000000")))
                .isEqualTo(Field.VEHICLE_YEAR);
        assertThat(fieldOf(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "abcd", "100000000", "3", "35000000")))
                .isEqualTo(Field.VEHICLE_YEAR);
        assertThat(fieldOf(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BEKAS, "200032", "100000000", "3", "25000000")))
                .isEqualTo(Field.VEHICLE_YEAR);
    }

    @Test
    void shouldReject_whenTotalLoanOutOfRange() {
        assertThat(fieldOf(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "2025", "0", "3", "0")))
                .isEqualTo(Field.TOTAL_LOAN);
        assertThat(fieldOf(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "2025", "1000000001", "3", "350000000")))
                .isEqualTo(Field.TOTAL_LOAN);
        assertThat(fieldOf(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "2025", "abc", "3", "35000000")))
                .isEqualTo(Field.TOTAL_LOAN);
    }

    @Test
    void shouldReject_whenHugeTotalLoanOverflowsParsing() {
        assertThat(fieldOf(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "2025", "99999999999999999999999", "3", "1")))
                .isEqualTo(Field.TOTAL_LOAN);
    }

    @Test
    void shouldReject_whenTenorOutOfRange() {
        assertThat(fieldOf(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "2025", "100000000", "0", "35000000")))
                .isEqualTo(Field.TENURE);
        assertThat(fieldOf(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "2025", "100000000", "7", "35000000")))
                .isEqualTo(Field.TENURE);
        assertThat(fieldOf(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "2025", "100000000", "tiga", "35000000")))
                .isEqualTo(Field.TENURE);
    }

    @Test
    void shouldReject_whenNewVehicleDownPaymentBelow35Percent() {
        final InvalidDownPaymentException failure = assertThrows(InvalidDownPaymentException.class,
                () -> validator.validate(
                        VehicleType.MOBIL, VehicleCondition.BARU, "2025", "100000000", "3", "34000000"));

        assertThat(failure.getField()).isEqualTo(Field.DOWN_PAYMENT);
    }

    @Test
    void shouldAccept_whenDownPaymentExactlyAtMinimum() {
        assertThat(validBaru().downPayment()).isEqualTo(35_000_000L);
        final LoanRequest bekas = validator.validate(
                VehicleType.MOTOR, VehicleCondition.BEKAS, "2015", "100000000", "2", "25000000");

        assertThat(bekas.downPayment()).isEqualTo(25_000_000L);
    }

    @Test
    void shouldReject_whenDownPaymentOneRupiahBelowMinimum() {
        assertThat(fieldOf(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "2025", "100000000", "3", "34999999")))
                .isEqualTo(Field.DOWN_PAYMENT);
        assertThat(fieldOf(() -> validator.validate(
                VehicleType.MOTOR, VehicleCondition.BEKAS, "2015", "100000000", "2", "24999999")))
                .isEqualTo(Field.DOWN_PAYMENT);
    }

    @Test
    void shouldReject_whenUsedVehicleDownPaymentBelow25Percent() {
        assertThat(fieldOf(() -> validator.validate(
                VehicleType.MOTOR, VehicleCondition.BEKAS, "2015", "100000000", "2", "24000000")))
                .isEqualTo(Field.DOWN_PAYMENT);
    }

    @Test
    void shouldReject_whenDownPaymentNotLessThanTotal() {
        assertThat(fieldOf(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "2025", "100000000", "3", "100000000")))
                .isEqualTo(Field.DOWN_PAYMENT);
        assertThat(fieldOf(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BEKAS, "2015", "100000000", "2", "200000000")))
                .isEqualTo(Field.DOWN_PAYMENT);
    }

    private static Field fieldOf(final RunnableWithFailure action) {
        try {
            action.run();
        } catch (final InvalidLoanInputException e) {
            return e.getField();
        }
        throw new AssertionError("expected InvalidLoanInputException");
    }

    private interface RunnableWithFailure {
        void run();
    }
}
