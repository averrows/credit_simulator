package com.bcadigital.creditsimulator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.bcadigital.creditsimulator.model.LoanRequest;
import com.bcadigital.creditsimulator.model.Rupiah;
import com.bcadigital.creditsimulator.model.VehicleCondition;
import com.bcadigital.creditsimulator.model.VehicleType;

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
        assertThat(request.totalLoanAmount()).isEqualTo(Rupiah.of(100_000_000L));
        assertThat(request.loanTenureYears()).isEqualTo(3);
        assertThat(request.downPayment()).isEqualTo(Rupiah.of(35_000_000L));
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

        assertThat(failure).hasMessage("Kendaraan baru: tahun minimal 2025");
    }

    @Test
    void shouldAccept_whenUsedVehicleHasAnyFourDigitYear() {
        final LoanRequest request = validator.validate(
                VehicleType.MOBIL, VehicleCondition.BEKAS, "1990", "100000000", "3", "25000000");

        assertThat(request.vehicleYear()).isEqualTo(1990);
    }

    @Test
    void shouldReject_whenYearIsNotFourDigits() {
        assertThatThrownBy(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "25", "100000000", "3", "35000000"))
                .isInstanceOf(InvalidYearException.class);
        assertThatThrownBy(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "20245", "100000000", "3", "35000000"))
                .isInstanceOf(InvalidYearException.class);
        assertThatThrownBy(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "abcd", "100000000", "3", "35000000"))
                .isInstanceOf(InvalidYearException.class);
        assertThatThrownBy(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BEKAS, "200032", "100000000", "3", "25000000"))
                .isInstanceOf(InvalidYearException.class);
    }

    @Test
    void shouldReject_whenTotalLoanOutOfRange() {
        assertThatThrownBy(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "2025", "0", "3", "0"))
                .isInstanceOf(InvalidTotalLoanException.class);
        assertThatThrownBy(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "2025", "1000000001", "3", "350000000"))
                .isInstanceOf(InvalidTotalLoanException.class);
        assertThatThrownBy(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "2025", "abc", "3", "35000000"))
                .isInstanceOf(InvalidTotalLoanException.class);
    }

    @Test
    void shouldReject_whenHugeTotalLoanOverflowsParsing() {
        assertThatThrownBy(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "2025", "99999999999999999999999", "3", "1"))
                .isInstanceOf(InvalidTotalLoanException.class);
    }

    @Test
    void shouldReject_whenTenorOutOfRange() {
        assertThatThrownBy(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "2025", "100000000", "0", "35000000"))
                .isInstanceOf(InvalidTenorException.class);
        assertThatThrownBy(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "2025", "100000000", "7", "35000000"))
                .isInstanceOf(InvalidTenorException.class);
        assertThatThrownBy(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "2025", "100000000", "tiga", "35000000"))
                .isInstanceOf(InvalidTenorException.class);
    }

    @Test
    void shouldReject_whenNewVehicleDownPaymentBelow35Percent() {
        final InvalidDownPaymentException failure = assertThrows(InvalidDownPaymentException.class,
                () -> validator.validate(
                        VehicleType.MOBIL, VehicleCondition.BARU, "2025", "100000000", "3", "34000000"));

        assertThat(failure).hasMessage("DP minimal 35% dari pinjaman (kondisi Baru)");
    }

    @Test
    void shouldAccept_whenDownPaymentExactlyAtMinimum() {
        assertThat(validBaru().downPayment()).isEqualTo(Rupiah.of(35_000_000L));
        final LoanRequest bekas = validator.validate(
                VehicleType.MOTOR, VehicleCondition.BEKAS, "2015", "100000000", "2", "25000000");

        assertThat(bekas.downPayment()).isEqualTo(Rupiah.of(25_000_000L));
    }

    @Test
    void shouldReject_whenDownPaymentOneRupiahBelowMinimum() {
        assertThatThrownBy(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "2025", "100000000", "3", "34999999"))
                .isInstanceOf(InvalidDownPaymentException.class);
        assertThatThrownBy(() -> validator.validate(
                VehicleType.MOTOR, VehicleCondition.BEKAS, "2015", "100000000", "2", "24999999"))
                .isInstanceOf(InvalidDownPaymentException.class);
    }

    @Test
    void shouldReject_whenUsedVehicleDownPaymentBelow25Percent() {
        assertThatThrownBy(() -> validator.validate(
                VehicleType.MOTOR, VehicleCondition.BEKAS, "2015", "100000000", "2", "24000000"))
                .isInstanceOf(InvalidDownPaymentException.class);
    }

    @Test
    void shouldReject_whenDownPaymentNotLessThanTotal() {
        assertThatThrownBy(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "2025", "100000000", "3", "100000000"))
                .isInstanceOf(InvalidDownPaymentException.class);
        assertThatThrownBy(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BEKAS, "2015", "100000000", "2", "200000000"))
                .isInstanceOf(InvalidDownPaymentException.class);
    }

    @Test
    void shouldAccept_whenMagnitudeSuffixes() {
        assertThat(validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "2025", "100 jt", "3", "35 jt")
                .totalLoanAmount()).isEqualTo(Rupiah.of(100_000_000L));
        assertThat(validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "2025", "0,1 m", "3", "35000 rb")
                .downPayment()).isEqualTo(Rupiah.of(35_000_000L));
        assertThat(validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "2025", "100000 rb", "3", "35 JUTA")
                .totalLoanAmount()).isEqualTo(Rupiah.of(100_000_000L));
        assertThat(validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "2025", "203.35 jt", "3", "100 jt")
                .totalLoanAmount()).isEqualTo(Rupiah.of(203_350_000L));
    }

    @Test
    void shouldAccept_whenSuffixAtMaxBoundary() {
        final LoanRequest request = validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "2025", "1 m", "6", "350 jt");

        assertThat(request.totalLoanAmount()).isEqualTo(Rupiah.of(1_000_000_000L));
        assertThat(request.downPayment()).isEqualTo(Rupiah.of(350_000_000L));
    }

    @Test
    void shouldReject_whenSuffixAmountInvalid() {
        assertThatThrownBy(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "2025", "1.5 m", "3", "1 m"))
                .isInstanceOf(InvalidTotalLoanException.class);
        assertThatThrownBy(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "2025", "100 jt", "3", "34.999999 jt"))
                .isInstanceOf(InvalidDownPaymentException.class);
        assertThatThrownBy(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "2025", "100 jt", "3", "0.0000001 jt"))
                .isInstanceOf(InvalidDownPaymentException.class);
        assertThatThrownBy(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "2025", "jt", "3", "35 jt"))
                .isInstanceOf(InvalidTotalLoanException.class);
        assertThatThrownBy(() -> validator.validate(
                VehicleType.MOBIL, VehicleCondition.BARU, "2025", "abc jt", "3", "35 jt"))
                .isInstanceOf(InvalidTotalLoanException.class);
    }

    @Test
    void shouldAccept_whenTenorHasLeadingZero() {
        assertThat(validator.parseTenor("06")).isEqualTo(6);
    }

    @Test
    void shouldRejectAsOutOfRange_whenTenorHasManyDigits() {
        assertThatThrownBy(() -> validator.parseTenor("99999999999999999999"))
                .isInstanceOf(InvalidTenorException.class)
                .hasMessage("Tenor harus 1 sampai 6 tahun");
    }
}
