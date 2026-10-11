package com.bcadigital.creditsimulator.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class VehicleTypeTest {

    @Test
    void shouldMatchExact_whenMixedCaseAndSurroundingWhitespace() {
        assertThat(VehicleType.parse("Mobil")).isEqualTo(new Match<>(VehicleType.MOBIL, false));
        assertThat(VehicleType.parse("  MOTOR ")).isEqualTo(new Match<>(VehicleType.MOTOR, false));
    }

    @Test
    void shouldMatchByPrefix_whenUniquePrefix() {
        assertThat(VehicleType.parse("mot")).isEqualTo(new Match<>(VehicleType.MOTOR, true));
        assertThat(VehicleType.parse("mob")).isEqualTo(new Match<>(VehicleType.MOBIL, true));
        assertThat(VehicleType.parse("Moto")).isEqualTo(new Match<>(VehicleType.MOTOR, true));
    }

    @Test
    void shouldReject_whenAmbiguousBlankOrUnknown() {
        assertThatThrownBy(() -> VehicleType.parse("mo")).isInstanceOf(InvalidChoiceException.class);
        assertThatThrownBy(() -> VehicleType.parse("m")).isInstanceOf(InvalidChoiceException.class);
        assertThatThrownBy(() -> VehicleType.parse("pesawat")).isInstanceOf(InvalidChoiceException.class);
        assertThatThrownBy(() -> VehicleType.parse("")).isInstanceOf(InvalidChoiceException.class);
        assertThatThrownBy(() -> VehicleType.parse(null)).isInstanceOf(InvalidChoiceException.class);
    }

    @Test
    void shouldNameBothCandidates_whenSharedPrefix() {
        assertThatThrownBy(() -> VehicleType.parse("mo")).hasMessage("\"mo\" ambigu (Mobil atau Motor?)");
    }
}
