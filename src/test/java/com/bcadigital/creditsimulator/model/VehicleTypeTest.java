package com.bcadigital.creditsimulator.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VehicleTypeTest {

    @Test
    void shouldMatchExact_whenMixedCaseAndSurroundingWhitespace() {
        assertThat(VehicleType.parse("Mobil")).contains(new Match<>(VehicleType.MOBIL, false));
        assertThat(VehicleType.parse("  MOTOR ")).contains(new Match<>(VehicleType.MOTOR, false));
    }

    @Test
    void shouldMatchByPrefix_whenUniquePrefix() {
        assertThat(VehicleType.parse("mot")).contains(new Match<>(VehicleType.MOTOR, true));
        assertThat(VehicleType.parse("mob")).contains(new Match<>(VehicleType.MOBIL, true));
        assertThat(VehicleType.parse("Moto")).contains(new Match<>(VehicleType.MOTOR, true));
    }

    @Test
    void shouldReturnEmpty_whenAmbiguousBlankOrUnknown() {
        assertThat(VehicleType.parse("mo")).isEmpty();
        assertThat(VehicleType.parse("m")).isEmpty();
        assertThat(VehicleType.parse("pesawat")).isEmpty();
        assertThat(VehicleType.parse("")).isEmpty();
        assertThat(VehicleType.parse(null)).isEmpty();
    }

    @Test
    void shouldListCandidates_whenSharedPrefix() {
        assertThat(VehicleType.candidates("mo")).containsExactlyInAnyOrder(VehicleType.MOBIL, VehicleType.MOTOR);
        assertThat(VehicleType.candidates("mot")).containsExactly(VehicleType.MOTOR);
        assertThat(VehicleType.candidates("xyz")).isEmpty();
        assertThat(VehicleType.candidates(null)).isEmpty();
    }
}
