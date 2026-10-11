package com.bcadigital.creditsimulator.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RupiahTest {

    @Test
    void shouldParsePlainDigits_whenNoSuffix() {
        assertThat(Rupiah.parse(" 100000000 ")).contains(Rupiah.of(100_000_000L));
    }

    @Test
    void shouldScaleBySuffix_whenSuffixed() {
        assertThat(Rupiah.parse("500 rb")).contains(Rupiah.of(500_000L));
        assertThat(Rupiah.parse("35JT")).contains(Rupiah.of(35_000_000L));
        assertThat(Rupiah.parse("0,1 m")).contains(Rupiah.of(100_000_000L));
        assertThat(Rupiah.parse("1 miliar")).contains(Rupiah.of(1_000_000_000L));
    }

    @Test
    void shouldTreatLastMarkAsDecimal_whenBothSeparatorsPresent() {
        assertThat(Rupiah.parse("203.35 jt")).contains(Rupiah.of(203_350_000L));
        assertThat(Rupiah.parse("203,35 jt")).contains(Rupiah.of(203_350_000L));
        assertThat(Rupiah.parse("1.000,5 rb")).contains(Rupiah.of(1_000_500L));
    }

    @Test
    void shouldReturnEmpty_whenNotWholeRepresentableRupiah() {
        assertThat(Rupiah.parse(null)).isEmpty();
        assertThat(Rupiah.parse("abc")).isEmpty();
        assertThat(Rupiah.parse("jt")).isEmpty();
        assertThat(Rupiah.parse("1.000.000")).isEmpty();
        assertThat(Rupiah.parse("0.0000001 jt")).isEmpty();
        assertThat(Rupiah.parse("99999999999999999999999")).isEmpty();
    }

    @Test
    void shouldCompareByPercentage_whenCheckingShare() {
        assertThat(Rupiah.of(35).isAtLeastPercentOf(35, Rupiah.of(100))).isTrue();
        assertThat(Rupiah.of(34).isAtLeastPercentOf(35, Rupiah.of(100))).isFalse();
    }

    @Test
    void shouldSubtractAndCompare_whenCombiningAmounts() {
        assertThat(Rupiah.of(100).minus(Rupiah.of(35))).isEqualTo(Rupiah.of(65));
        assertThat(Rupiah.of(2).isGreaterThan(Rupiah.of(1))).isTrue();
        assertThat(Rupiah.of(1).isGreaterThan(Rupiah.of(1))).isFalse();
    }

    @Test
    void shouldReject_whenNegative() {
        assertThatThrownBy(() -> Rupiah.of(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Rupiah.of(1).minus(Rupiah.of(2))).isInstanceOf(IllegalArgumentException.class);
    }
}
