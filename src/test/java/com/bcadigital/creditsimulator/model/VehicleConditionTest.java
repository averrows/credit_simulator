package com.bcadigital.creditsimulator.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class VehicleConditionTest {

    @Test
    void shouldMatchExact_whenMixedCaseAndSurroundingWhitespace() {
        assertThat(VehicleCondition.parse("Baru")).isEqualTo(new Match<>(VehicleCondition.BARU, false));
        assertThat(VehicleCondition.parse("  BEKAS ")).isEqualTo(new Match<>(VehicleCondition.BEKAS, false));
    }

    @Test
    void shouldMatchByPrefix_whenUniquePrefix() {
        assertThat(VehicleCondition.parse("bek")).isEqualTo(new Match<>(VehicleCondition.BEKAS, true));
        assertThat(VehicleCondition.parse("bar")).isEqualTo(new Match<>(VehicleCondition.BARU, true));
        assertThat(VehicleCondition.parse("be")).isEqualTo(new Match<>(VehicleCondition.BEKAS, true));
        assertThat(VehicleCondition.parse("ba")).isEqualTo(new Match<>(VehicleCondition.BARU, true));
    }

    @Test
    void shouldReject_whenAmbiguousBlankOrUnknown() {
        assertThatThrownBy(() -> VehicleCondition.parse("b")).isInstanceOf(InvalidChoiceException.class);
        assertThatThrownBy(() -> VehicleCondition.parse("seken")).isInstanceOf(InvalidChoiceException.class);
        assertThatThrownBy(() -> VehicleCondition.parse("  ")).isInstanceOf(InvalidChoiceException.class);
        assertThatThrownBy(() -> VehicleCondition.parse(null)).isInstanceOf(InvalidChoiceException.class);
    }

    @Test
    void shouldNameBothCandidates_whenSharedPrefix() {
        assertThatThrownBy(() -> VehicleCondition.parse("b")).hasMessage("\"b\" ambigu (Baru atau Bekas?)");
    }
}
